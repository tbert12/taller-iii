from google.appengine.api import memcache
from taxitracker.helper import handler_helper
from taxitracker.model import travel
from taxitracker import config
import logging

def _get_stats_memcachekey(page_size, cursor):
    return "stats:page_size-{}:cursor-{}".format(page_size,"init" if cursor == None else cursor)

def get_stats(request):
    logging.info("Get Stats")
    
    page_size = request.args.get('page_size', default = config.store['DEFAULT_PAGE_SIZE'], type = int)
    cursor = request.args.get('cursor', default = None)
    
    cache_key = _get_stats_memcachekey(page_size, cursor)
    cache = memcache.get(cache_key)
    if (cache != None):
        logging.info("Read stats from MemCache [{}]".format(cache_key))
        return handler_helper.response_from_json(cache)
    
    stats = travel.DayTravelCounter._query_day(page_size, cursor)

    memcache.add(cache_key, stats, config.store["MEMCACHE_STATS_EXPIRATION_SEC"])
    logging.info("Stored stats in MemCache [{}]".format(cache_key))

    return handler_helper.response_from_json(stats)

def _get_args_admin_stats(request):
    return {
        'page_size' : request.args.get('page_size', default = config.store['DEFAULT_PAGE_SIZE'], type = int),
        'cursor' : request.args.get('cursor', default = None, type = str),
        'vendorID' : request.args.get('vendorID', default = None, type = int),
        'from_date' : request.args.get('from_date', default = None, type = str),
        'to_date' : request.args.get('to_date', default = None, type = str),
    }

def get_admin_stats(request):
    logging.info("Get admin stats")

    args = _get_args_admin_stats(request)
    admin_stats = travel.Travel._query_admin(args)

    return handler_helper.response_from_json(admin_stats)