from flask import Flask, jsonify
import json
from google.appengine.api import taskqueue
from src import store
import logging

app = Flask(__name__)

@app.before_request
def enable_local_error_handling():
    app.logger.addHandler(logging.StreamHandler())
    app.logger.setLevel(logging.DEBUG)

@app.route('/task/_travel_counter')
def counter():
    logging.info("Add counter day task")
    queue = taskqueue.Queue('travels-count-queue')

    tasks = queue.lease_tasks(20, 100) # Take 100 tasks for 20 sec

    store.add_travels_count(len(tasks))

    queue.delete_tasks(tasks)
    return jsonify({'success':True})

@app.route('/task/_billing_accumulator')
def billing_accumulator():
    logging.info("Billing accumulator")
    queue = taskqueue.Queue('billing-queue')

    tasks = queue.lease_tasks(100, 100)

    for task in tasks:
        store.add_billing(json.loads(task.payload))
    
    queue.delete_tasks(tasks)
    return jsonify({'success':True})
    