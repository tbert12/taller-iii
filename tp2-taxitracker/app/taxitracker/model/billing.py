from google.appengine.ext import ndb
from taxi import Taxi

class DayBilling(ndb.Model):
    taxi = ndb.KeyProperty(kind=Taxi)
    date = ndb.DateProperty()
    total = ndb.FloatProperty()
    count = ndb.IntegerProperty()