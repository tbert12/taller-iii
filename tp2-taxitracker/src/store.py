from google.cloud import datastore

datastore_client = datastore.Client()

def register_taxi(name):
    # Generate new Entity with taxi driver. return id
    key = dat
    datastore_client.put()

def track_travel(**kwargs):
    # Generate new Entity with data to travel. return bool


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
