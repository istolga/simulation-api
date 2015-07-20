# simulation-api

Api simulation framework on top of Netty 4 (NIO client server framework) and Spring framework.

The goal is to simulate slow responding third party apis. 

## Usage
Each api is configured in resources/sim-api-config.xml

If there is splunk(or other log system) results for api latencies use the following splunk query(last month):

        users/1234 | stats count(tet) as ctet, by tet | sort by ctet desc
        
take 10-15 buckets and then:

       users/1234 | stats count by tet | rangemap field=tet 1=0-50 2=51-100 3=101-600 4=601-1100 5=1101-1600 6=1601-2100 7=2101-2600 8=2601-3100 9=3101-3600 10=3601-4200 11=4201-4700 default=12 | stats avg(tet), count by range

## Details
Configuration is done by the following:

     <mapping uri="/public/v7/users/1234" reject-type="REJECT" throuput-in-min="1800" filename="response/user1234.xml">
                <latencies>
                    <latency percent="12" timems="16"></latency>
                    <latency percent="38" timems="401"></latency>
                    <latency percent="33" timems="873"></latency>
                    <latency percent="7" timems="1535"></latency>
                    <latency percent="141" timems="1897"></latency>
                    <latency percent="127" timems="2332"></latency>
                    <latency percent="57" timems="2803"></latency>
                    <latency percent="19" timems="3284"></latency>
                    <latency percent="7" timems="3706"></latency>
                    <latency percent="4" timems="4395"></latency>
                    <latency percent="2" timems="4976"></latency>
                </latencies>
      </mapping>
where **reject-type** is **REJECT** or **WAIT**. 
If number of requests greater than throuput-in-min and REJECT type is specified - 429("Too Many Requests") http status will be sent. 
If type is WAIT - the server will wait for available throuput, in this case for the next minute.

**filename** is a response file located at resources/response
If filename doesn't have an extension, for example filename="response/user", it will depend on Content-Type header.
If Content-Type is "application/xml" - response/user.xml will be used, "application/json" - response/user.json will be used.

**latency** defines how long to hold a server in ms. Percent is in how many cases such latency should happen. 
It can be in any units, what's important is to just have the same units for all latencies.
            