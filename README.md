# Messaging Service

### Endpoint parameters
base url = {$kube_url}/messaging/  
kube_url = nimble.eu-de.containers.appdomain.cloud  
port = 8080

verify messaging service is up and running:
-------------------------------------------
Get /  
should return Hello from communication service  
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/'

health check of messaging service:
----------------------------------
Get /health-check  
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/health-check'

starts new session:
-------------------
Post /start-new - query params: id1, id2  
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/start-new/?id1=nir&id2=benny'

send a new message (as part of a session):
------------------------------------------
Post /{session_id}/send - query params: source, target, message  
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/send?source=nir&target=benny&message=test_message'

get all user id sessions:
-------------------------
Get /{user_id}/sessions  
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/nir/sessions'

get latest message sent by the source in this session:
------------------------------------------------------
Get /{session_id}/latest - query params source, target  
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/latest?source=nir&target=benny'

get all messages sent from the source to target in this session
------------------------------------------------------
Get /{session_id}/all - query params source, target  
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/all?source=nir&target=benny'


archive session:
----------------
Post /{session_id}/archive - query params id1, id2  
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/archive?id1=nir&id2=benny'
