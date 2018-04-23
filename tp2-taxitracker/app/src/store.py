from google.appengine.ext import ndb

class Taxi(ndb.Model):
    taxi_id = ndb.IntegerProperty()

class Travel(ndb.Model):
    

def register_taxi(name):
    # Generate new Entity with taxi driver. return id
    return True

def track_travel(**kwargs):
    # Generate new Entity with data to travel. return bool
    return True

def get_billing(taxi_id):
    # TODO: Memcache (?)
    return 234

def get_stats():
    # TODO: Memcache or Datastore
    return []

def get_admin_stats(**kwargs):
    #TODO: Memcache or datastore
    taxi_id = kwargs.get('taxi_id', None)
    filter_from = kwargs.get('from')
    filter_to = kwargs.get('to')
    return []
