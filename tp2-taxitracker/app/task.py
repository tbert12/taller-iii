from flask import Flask
import logging
from taxitracker.handler import travel_handler, billing_handler

app = Flask(__name__)

app.logger.addHandler(logging.StreamHandler())
app.logger.setLevel(logging.DEBUG)


@app.route('/task/_travel_counter')
def counter():
    return travel_handler.process_travel_tasks()


@app.route('/task/_billing_accumulator')
def billing_accumulator():
    return billing_handler.process_billing_tasks()

