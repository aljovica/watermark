To run and build (with tests) the application just run the following command:
```
./build-and-run-app.sh
```
To application is running on http://localhost:8080, but it can be changed in application-configuration.json file.

The watermark process is simulated with a timer. The timer can be configured in the same config file.

Three endpoints are exposed:

1. POST /v1/document with a following body:
    ```
    {
      "content": "journal",
      "title": "The Dark Code",
      "author": "Bruce Wayne"
    }
    ```
    It is returning an id in this format:
    ```
    {
      "id": "6f3769db-8ba4-4071-b9f8-aa8d08de06b7"
    }
    ```
    
2. HEAD /v1/document/{id} 

    Has no body, but writes a header named "watermarked" with either "done" or "in progress". The id parameter is 
    the result of the previous request. 
    
3. GET /v1/document/{id} is returning following body:
    ```
    {
      "watermark": "watermark-journal-Bruce Wayne-The Dark Code",
      "title": "The Dark Code",
      "author": "Bruce Wayne",
      "content": "journal"
    }
    ```