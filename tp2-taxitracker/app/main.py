from flask import Flask, request, jsonify
from taxitracker import store, config
import logging

app = Flask(__name__)

DEFAULT_PAGE_SIZE = config.store["DEFAULT_PAGE_SIZE"]

@app.before_request
def enable_local_error_handling():
    app.logger.addHandler(logging.StreamHandler())
    app.logger.setLevel(logging.DEBUG)


@app.route('/api/')
def index():
    return "Rest API Taxi Tracker v1"

@app.route('/api/register', methods = ["POST"])
def register():
    response = {'success':False}
    code = 400
    if request.method == "POST" and request.json != None:
        if "vendorID" in request.json:
            # Create taxi. Arguments id or generate id
            if store.register_taxi(request.json['vendorID']) != None:
                response["success"] = True
                code = 200
            else: 
                response["info"] = "Internal error while creating taxi"
        else:
            response["info"] = "No `vendorID` in body data"
    else:
        response['info'] = "Method should be POST and have vendorID in JSONBody"
    
    return jsonify(response), code

@app.route('/api/track', methods = ["POST"])
def track():
    response = {'success': False, 'info': "Request method should be POST"}
    code = 404
    if request.method == "POST" and request.json != None:
        logging.info("Track state of travel")
        result, info = store.track_travel(request.json)
        response['success'] = result
        response['info'] = info
    return response, code

@app.route('/api/billing')
def billing():
    # Return day billig of taxi_id
    logging.info("Get billing of day of taxi")
    vendorID = request.args.get('vendorID', default = None, type = int)
    if vendorID != None:
        total, info = store.get_billing(vendorID)
        return jsonify({
            'total' : total,
            'info' : info
        })
    return jsonify({
        'error' : "Not argument vendorID to get billing"
    })

@app.route('/api/stats')
def stats():
    logging.info("Get stats of days")
    # Travels per day
    args = {
        'page_size' : request.args.get('page_size', default = DEFAULT_PAGE_SIZE, type = int),
        'cursor' : request.args.get('cursor', default = None)
    }
    stats= store.get_stats(args)
    return jsonify(stats)


@app.route('/api/admin_stats')
def stats_admin():
    # Gets data from filter
    logging.info("Get Admin stats")
    stats = store.get_admin_stats({
        'page_size' : request.args.get('page_size', default = DEFAULT_PAGE_SIZE, type = int),
        'cursor' : request.args.get('cursor', default = None, type = str),
        'vendorID' : request.args.get('vendorID', default = None, type = int),
        'from_date' : request.args.get('from_date', default = None, type = str),
        'to_date' : request.args.get('to_date', default = None, type = str),
    })
    return jsonify(stats)