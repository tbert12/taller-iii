from google.appengine.ext import ndb

class Taxi(ndb.Model):
    vendorID = ndb.IntegerProperty()

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

class DayBilling(ndb.Model):
    taxi = ndb.KeyProperty(kind=Taxi)
    date = ndb.DateProperty()
    total = ndb.FloatProperty()
    count = ndb.IntegerProperty()

class DayTravelCounter(ndb.Model):
    date = ndb.DateProperty()
    count = ndb.IntegerProperty()