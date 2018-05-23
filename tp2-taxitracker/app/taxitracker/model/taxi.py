from google.appengine.ext import ndb
class Taxi(ndb.Model):
    vendorID = ndb.IntegerProperty()