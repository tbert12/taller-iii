from taxitracker.model.taxi import Taxi
from taxitracker.helper import handler_helper
import logging


def register_taxi(request):
    logging.info("Register taxi")
    if request.json != None:
        if "vendorID" in request.json:
            vendorID = request.json["vendorID"]
            taxi = Taxi(
                id = vendorID,
                vendorID = vendorID,
            )
            logging.info("Registered Taxi: {}".format(vendorID))
            if taxi.put():
                return handler_helper.response_from_boolean(True, "Created taxi with vendorID: {}".format(vendorID))
            else: 
                return handler_helper.response_from_boolean(False, "Internal error while creating taxi: {}".format(vendorID))
        else:
            return handler_helper.response_from_boolean(False,  "No `vendorID` in body data")
    
    return handler_helper.response_from_boolean(False, "No Body data. Expected: { `vendorID` : int }")

    
    