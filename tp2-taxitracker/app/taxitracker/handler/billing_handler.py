import logging
from google.appengine.api import memcache
from taxitracker.model import billing, taxi
from google.appengine.api import taskqueue
from taxitracker.helper import handler_helper
from taxitracker import config
import datetime
import json

def _get_or_insert_daybilling(**kwargs):
    if 'vendorID' in kwargs:
        vendor_taxi = taxi.Taxi.get_by_id(kwargs.get('vendorID'))
        if vendor_taxi != None:
            today = datetime.date.today()
            day_billing = billing.DayBilling.get_or_insert( 
                str(today) + "_" + str(vendor_taxi.vendorID), # Key from today and vendorID
                taxi = vendor_taxi.key,
                date = today,
                total = 0.0,
                count = 0)
            return day_billing
    return None

def get_billing(request):
    vendorID = request.args.get('vendorID', default = None, type = int)
    if vendorID == None:
        return handler_helper.response_from_boolean(False, "Expected vendorID:int in query")
    
    logging.info("Get billing day of {}".format(vendorID))
    
    json_response = {
        'billing' : 0,
        'info' : "Billing of the day of vendorID: {}".format(vendorID)
    }
    cache_key = "billing-{}".format(vendorID)
    cache = memcache.get(cache_key)
    if (cache != None):
        json_response['billing'] = cache
        return handler_helper.response_from_json(json_response)
    
    day_billing = _get_or_insert_daybilling(vendorID = vendorID)
    if day_billing != None:
        memcache.add(cache_key, day_billing.total, config.store["MEMCACHE_BILLING_EXPIRATION_SEC"])
        json_response['billing'] = day_billing.total
        return handler_helper.response_from_json(json_response)
    
    return handler_helper.response_from_boolean(False, "Invalid vendorID {}".format(vendorID))


def _add_billing(data):
    logging.info("Add billing to vendorID {}".format(data['vendorID']))
    day_billing = _get_or_insert_daybilling(vendorID = data['vendorID'])
    if day_billing != None:
        day_billing.count += data["total_count"]
        day_billing.total += data["total_amount"]
        day_billing.put()

def _accumullate_tasks(tasks):
    tasks_data_vendorID = {}
    for task in tasks:
        print task.payload, type(task.payload)
        data = json.loads(task.payload)
        vendorID = data["vendorID"]
        total = data["total"]
        if vendorID in tasks_data_vendorID:
            tasks_data_vendorID[vendorID]["total_amount"] += data["total"]
            tasks_data_vendorID[vendorID]["total_count"] += 1
        else:
            tasks_data_vendorID[vendorID] = {
                'vendorID' : vendorID,
                'total_count' : 1,
                'total_amount' : total 
            }
    return list(tasks_data_vendorID.values())

def process_billing_tasks():
    logging.info("Billing accumulator")

    conf = config.task["billing_accumulator"]

    queue = taskqueue.Queue(conf["QUEUE_NAME"])
    tasks = queue.lease_tasks(conf["LEASE_TASK_TIME_SEC"], conf["LEASE_TASK_COUNT"])
    for data in _accumullate_tasks(tasks):
        _add_billing(data)
    
    queue.delete_tasks(tasks)
    return handler_helper.response_from_boolean(True)