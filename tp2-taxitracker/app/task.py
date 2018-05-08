from flask import Flask, jsonify
import json
from google.appengine.api import taskqueue
from taxitracker import store, config
import logging


app = Flask(__name__)

app.logger.addHandler(logging.StreamHandler())
app.logger.setLevel(logging.DEBUG)

@app.route('/task/_travel_counter')
def counter():
    logging.info("Add counter day task")
    queue = taskqueue.Queue('travels-count-queue')

    conf = config.task["travel_counter"]

    tasks = queue.lease_tasks(conf["LEASE_TASK_TIME_SEC"], conf["LEASE_TASK_COUNT"]) # Take 100 tasks for 20 sec

    store.add_travels_count(len(tasks))

    queue.delete_tasks(tasks)
    return jsonify({'success':True})

@app.route('/task/_billing_accumulator')
def billing_accumulator():
    logging.info("Billing accumulator")
    queue = taskqueue.Queue('billing-queue')

    conf = config.task["billing_accumulator"]
    tasks = queue.lease_tasks(conf["LEASE_TASK_TIME_SEC"], conf["LEASE_TASK_COUNT"])

    for task in tasks:
        store.add_billing(json.loads(task.payload))
    
    queue.delete_tasks(tasks)
    return jsonify({'success':True})
    