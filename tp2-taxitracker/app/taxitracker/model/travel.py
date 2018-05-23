from google.appengine.ext import ndb
from taxi import Taxi
from taxitracker import config
import datetime


TRAVEL_STATUSES = ("PICKUP","DROPOFF")


class Travel(ndb.Model):
    taxi = ndb.KeyProperty(kind=Taxi)
    state = ndb.StringProperty() # PICKUP, DROPOFF
    date = ndb.DateTimeProperty()
    amount = ndb.FloatProperty()
    geo_position = ndb.GeoPtProperty() # ndb.GeoPt(lat,lon)

    def to_dict(self):
        return {
            'taxi' : self.taxi.get().vendorID,
            'state' : self.state,
            'date' : self.date,
            'amount' : self.amount,
            'position' : {
                'latitude' : self.geo_position.lat,
                'longitude' : self.geo_position.lon
            }
        }
    
    @classmethod
    def _query_admin(cls, args):
        page_size = args.get('page_size', config.store["DEFAULT_PAGE_SIZE"])
        cursor = args.get('cursor', None)
        query = cls.query().order(-cls.date, cls.key)
        if 'vendorID' in args and args['vendorID'] != None:
            query = query.filter( cls.taxi == ndb.Key("Taxi",args["vendorID"]) )
        if 'from_date' in args and args['from_date'] != None:
            query = query.filter(cls.date >= datetime.datetime.strptime(args['from_date'], "%Y-%m-%dT%H:%M:%S.000Z"))
        if 'to_date' in args and args['to_date'] != None:
            query = query.filter(cls.date <= datetime.datetime.strptime(args['to_date'], "%Y-%m-%dT%H:%M:%S.000Z"))
        
        if (cursor == None):
            result, more_cursor, more = query.fetch_page(page_size)
        else:
            result, more_cursor, more = query.fetch_page(page_size, start_cursor = ndb.Cursor.from_websafe_string(cursor) )
        
        more_cursor = more_cursor.to_websafe_string() if more else None
        
        return {
            'stats' : [ r.to_dict() for r in result ], 
            'cursor' : more_cursor
        }


class DayTravelCounter(ndb.Model):
    date = ndb.DateProperty()
    count = ndb.IntegerProperty()

    @classmethod
    def _query_day(cls, page_size = config.store["DEFAULT_PAGE_SIZE"], cursor = None):
        query = cls.query().order(-cls.date, cls.key)
        if (cursor == None):
            result, more_cursor, more = query.fetch_page(page_size)
        else:
            result, more_cursor, more = query.fetch_page(page_size, start_cursor = ndb.Cursor.from_websafe_string(cursor) )
        
        more_cursor = more_cursor.to_websafe_string() if more else None
    
        return {
            'stats' : [r.to_dict() for r in result], 
            'cursor' : more_cursor
        }