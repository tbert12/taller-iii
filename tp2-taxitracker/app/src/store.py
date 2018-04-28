from google.appengine.ext import ndb
from google.appengine.api import memcache
from google.appengine.api import taskqueue
import json
import logging
import model
import datetime
import config

def register_taxi(vendorID):
    logging.info("Register taxi")
    taxi = model.Taxi(
        id = vendorID,
        vendorID = vendorID,
    )
    return taxi.put()


def add_task_travel_queue(args):
    travel_count_queue = taskqueue.Queue('travels-count-queue')
    tasks = []
    tasks.append( taskqueue.Task(payload="dummy: one travel", method='PULL') )
    travel_count_queue.add(tasks)
    if 'vendorID' in args and 'total' in args:
        billing_queue = taskqueue.Queue('billing-queue')
        tasks = []
        tasks.append(taskqueue.Task(payload=json.dumps(args), method='PULL'))
        billing_queue.add(tasks)        

track_travel_args = ("vendorID","status","time","position")
def track_travel(json_data):
    logging.info("track travel")
    if all (key in json_data for key in track_travel_args):
        lat = json_data['position']['latitude']
        lon = json_data['position']['longitude']
        if json_data['status'] in model.TRAVEL_STATUSES:
            taxi_key = model.Taxi.get_by_id(json_data['vendorID'])

            if taxi_key != None:
                travel = model.Travel(
                    taxi = taxi_key.key,
                    state = json_data['status'],
                    amount = json_data['amount'] if 'amount' in json_data else 0.0,
                    date = datetime.datetime.strptime(json_data['time'], "%Y-%m-%d %H:%M:%S"),
                    geo_position = ndb.GeoPt(lat, lon)
                )
                travel.put()
                if travel.state == "DROPOFF": 
                    add_task_travel_queue({'vendorID' : travel.taxi.get().vendorID, 'total' : travel.amount})
            else:
                return False, "taxi {} not exist".format(vendorID)
        else:
            return False, "Invalid status: {}".format(travel_status)
    else:
        return False, "Invalid arguments"
    return True, "Travel added"

def _get_billing(**kwargs):
    if 'vendorID' in kwargs:
        taxi = model.Taxi.get_by_id(kwargs.get('vendorID'))
        if taxi != None:
            today = datetime.date.today()
            day_billing = model.DayBilling.get_or_insert( 
                str(today) + "_" + str(taxi.vendorID), # Key from today and vendorID
                taxi = taxi.key,
                date = today,
                total = 0.0,
                count = 0)
            return day_billing
    return None

def add_billing(data):
    logging.info("Add billing to vendorID {}".format(data['vendorID']))
    day_billing = _get_billing(vendorID = data['vendorID'])
    if day_billing != None:
        day_billing.count += 1
        day_billing.total += data["total"]
        day_billing.put()

def get_billing(vendorID):
    logging.info("Get billing of {}".format(vendorID))
    cache = memcache.get(str(vendorID))
    if (cache != None):
        return cache, "Billing for today ({}) of vendorID: {}".format(str(day_billing.date), vendorID)
    day_billing = _get_billing(vendorID = vendorID)
    if day_billing != None:
        memcache.add(str(vendorID), day_billing.total, config.store["MEMCACHE_BILLING_EXPIRATION_SEC"])
        return day_billing.total, "Billing for today ({}) of vendorID: {}".format(str(day_billing.date), vendorID)
    return 0, "Invalid vendorID {}".format(vendorID)

def get_stats_memcachekey(page_size, cursor):
    return "stats:page_size-{}:cursor-{}".format(page_size,"init" if cursor == None else cursor)

def get_stats(args):
    logging.info("Get Stats")
    
    page_size = args['page_size'] if 'page_size' in args else config.store["DEFAULT_PAGE_SIZE"]
    cursor = args['cursor'] if 'cursor' in args  else None
    
    cache_key = get_stats_memcachekey(page_size, cursor)
    cache = memcache.get(cache_key)
    if (cache != None):
        logging.info("Read stats from MemCache")
        return cache

    query = model.DayTravelCounter.query().order(-model.DayTravelCounter.date, model.DayTravelCounter.key)
    if (cursor == None):
        result, more_cursor, more = query.fetch_page(page_size)
    else:
        result, more_cursor, more = query.fetch_page(page_size, start_cursor = ndb.Cursor.from_websafe_string(cursor) )
    more_cursor = more_cursor.to_websafe_string() if more else None
    
    to_return = {
        'stats' : [r.to_dict() for r in result], 
        'cursor' : more_cursor
    }

    memcache.add(cache_key, to_return, config.store["MEMCACHE_STATS_EXPIRATION_SEC"])
    logging.info("Stored stats in MemCache")

    return to_return

def get_admin_stats(args):
    logging.info("Get admin stats")

    page_size = args['page_size'] if 'page_size' in args else 10
    cursor = args['cursor'] if 'cursor' in args else None
    query = model.Travel.query().order(-model.Travel.date, model.Travel.key)
    if 'vendorID' in args and args['vendorID'] != None:
        query = query.filter( model.Travel.taxi == model.Taxi.get_by_id(args['vendorID']).key )
    if 'from_date' in args and args['from_date'] != None:
        query = query.filter(model.Travel.date >= datetime.datetime.strptime(args['from_date'], "%Y-%m-%dT%H:%M:%S.000Z"))
    if 'to_date' in args and args['to_date'] != None:
        query = query.filter(model.Travel.date <= datetime.datetime.strptime(args['to_date'], "%Y-%m-%dT%H:%M:%S.000Z"))
    
    if (cursor == None):
        result, more_cursor, more = query.fetch_page(page_size)
    else:
        result, more_cursor, more = query.fetch_page(page_size, start_cursor = ndb.Cursor.from_websafe_string(cursor) )
    more_cursor = more_cursor.to_websafe_string() if more else None
    

    return {
        'stats' : [ r.to_dict() for r in result ], 
        'next_page' : more_cursor
    }


def add_travels_count(count):
    logging.info("Adding {} travels to day count".format(count))
    if count == 0:
        return
    
    today = datetime.date.today()
    day_count = model.DayTravelCounter.get_or_insert( 
        str(today), # Key from today
        date=today,
        count=0)
    day_count.count += count
    day_count.put()
    
    logging.info("Added {} travels to {}".format(count, str(today)))
