import logging
from taxitracker.model.taxi import Taxi
from taxitracker.model.travel import Travel, DayTravelCounter, TRAVEL_STATUSES
from taxitracker import config
import datetime
from taxitracker.helper import handler_helper
from google.appengine.api import taskqueue
from google.appengine.ext import ndb
import json


def _add_task_in_queue(queue_name, payload = ''):
    queue = taskqueue.Queue(queue_name)
    tasks = []
    tasks.append( taskqueue.Task(payload = payload, method = 'PULL' ) )
    queue.add(tasks)
    logging.info("Added task in queue '{}'".format(queue_name))

def _generate_task(args):
    _add_task_in_queue(config.task['travel_counter']["QUEUE_NAME"])
    _add_task_in_queue(config.task['billing_accumulator']["QUEUE_NAME"], json.dumps(args))     

def track_travel(request):
    logging.info("Track travel")
    if (request.json == None):
        return handler_helper.response_from_boolean(False, "No body in request")
    
    json_data = request.json
    if all(key in json_data for key in ("vendorID","type","date","position")):
        if json_data['type'] in TRAVEL_STATUSES:
            taxi = Taxi.get_by_id(json_data['vendorID'])
            if taxi != None:
                lat = json_data['position']['latitude']
                lon = json_data['position']['longitude']
                travel = Travel(
                    taxi = taxi.key,
                    state = json_data['type'],
                    amount = float(json_data['amount']) if 'amount' in json_data and len(str(json_data['amount']))>0 else 0.0,
                    date = datetime.datetime.strptime(json_data['date'], "%Y-%m-%d %H:%M:%S"),
                    geo_position = ndb.GeoPt(lat, lon)
                )
                travel.put()
                if travel.state == "DROPOFF": 
                    _generate_task({'vendorID' : travel.taxi.get().vendorID, 'total' : travel.amount})
                
                logging.info("Create travel status to vendorID: {}".format(json_data['vendorID']))
                return handler_helper.response_from_boolean(True, "Travel added to vendoirID: {}".format(json_data["vendorID"]))
            
            return handler_helper.response_from_boolean(False, "Error on track. Taxi {} not exist".format(json_data['vendorID']))
        
        return handler_helper.response_from_boolean(False, "Error on track. Invalid travel type: {}".format(json_data['type']))
    
    return handler_helper.response_from_boolean(False, "Invalid arguments. Check body data")

def _add_travels_count(count):
    if count == 0:
        return
    
    today = datetime.date.today()
    day_count = DayTravelCounter.get_or_insert( 
        str(today), # Key from today
        date=today,
        count=0)
    day_count.count += count
    day_count.put()
    
    logging.info("Added {} travels to {}".format(count, str(today)))


def process_travel_tasks():
    logging.info("Add counter day task")

    conf = config.task["travel_counter"]

    queue = taskqueue.Queue( conf["QUEUE_NAME"] )

    tasks = queue.lease_tasks(conf["LEASE_TASK_TIME_SEC"], conf["LEASE_TASK_COUNT"]) # Take 100 tasks for 20 sec

    _add_travels_count(len(tasks))

    queue.delete_tasks(tasks)
    return handler_helper.response_from_boolean(True, "Added {} travels".format(len(tasks)))