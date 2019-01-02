# Messaging Service

### Endpoint parameters
base url = {$kube_url}/messaging/  
kube_url = nimble.eu-de.containers.appdomain.cloud  
port = 8080

verify messaging service is up and running:
-------------------------------------------
Get /  
should return Hello from communication service  
```
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/'
Hello from communication service
```

health check of messaging service:
----------------------------------
Get /health-check  
should return OK  
```
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/health-check'
OK
```

starts new session:
-------------------
Post /start-new - query params: id1, id2  
the call returns the newly created session id  
```
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/start-new/?id1=nir&id2=benny'
771816529
```

send a new message (as part of a session):
------------------------------------------
Post /{session_id}/send - query params: source, target, message  
the call should return the following message: "MessageData was sent"  
```
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/send?source=nir&target=benny&message=test_message'
MessageData was sent
```

get all user id sessions:
-------------------------
Get /{user_id}/sessions  
returns an array of all user sessions  
```
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/nir/sessions'
[{"active":true,"sid":771816529,"cid":"nir109079benny"}]
```

get latest message sent by the source in this session:
------------------------------------------------------
Get /{session_id}/latest - query params source, target  
```
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/latest?source=nir&target=benny'
test_message
```

get all messages sent from the source to target in this session
------------------------------------------------------
Get /{session_id}/all - query params source, target  
```
curl -X GET 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/all?source=nir&target=benny'
["test_message"]
```

archive session:
----------------
Post /{session_id}/archive - query params id1, id2  
once a seesion is archived it's not possible to sent messages using the session  
```
curl -X POST 'https://nimble.eu-de.containers.appdomain.cloud/messaging/771816529/archive?id1=nir&id2=benny'
Collaboration with id 771816529 was archived
```
