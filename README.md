 
<pre>
                      ________    ______
                     / ____/ /   / ____/
                    / __/ / /   / /_    
                   / /___/ /___/ __/    
                  /_____/_____/_/   
                  
           A SDN Network Management for Hadoop
</pre>                    


   Install
===============

Elf can be built from source code. 

* Build elf service.
 
 1. Prerequisites
    
    Java JDK. (Our tests are based on sun JDK 1,6, other version e.g. 1.7 or openjdk should work as well, but not tested). You can download JDK 1.6 from http://tinyurl.com/7w2jeyl, and follow the install instruction there to get Java ready.
    
    Apache Ant. Install by: apt-get install ant

 2. Compile
 
    Go to elf/floodlight dir
 
    Compile by: ant clean; ant

 3. Start Elf service
 
    Go to elf/floodlight dir

    Start as normal floodlight controller: 
    
        java -jar target/floodlight.jar
        
* Build Hadoop-elf patch
 
 1. Prerequisites: the same as 1.1

 2. Compile
 
    Go to elf/hadoop dir

    Compile by:
        
        ant clean; ant
        

  Running elf
===============

As a user case, we have built a multi-datacenter aware Hadoop.
To enable this functionality, you need to:
* Set up Hadoop cluster as normal, e.g. configure conf/*.xml and conf/masters conf/slaves. This is the basic configuration of Hadoop.
* In conf/core-site.xml, add the following property:
 
      <property>
              <name>topology.node.switch.mapping.impl</name>
              <value>org.apache.hadoop.net.SDNTopologyManager</value>
     </property>
 
* Use REST API to register network topology with elf service:
    1. Register root switch of the network: 
  
        curl -d '["the dpid of root switch"]' http://controller ip:controller port/wm/hadooptopology/root/json 
      e.g. the root dpid is 00:00:00:00:00:00:00:01, your elf service (floodlight controller) is running on localhost:8080,
        then this registration should be like: 
 
        curl -d '["00:00:00:00:00:00:00:01"]' http://localhost:8080/wm/hadooptopology/root/json    

    2. Register the gateway switches

        curl -d '{"datacenter id":["gateway1 dpid", "gateway2 dpid" ... ], ... }' http://controller ip:controller port/wm/hadooptopology/gateway/json
     e.g. there are 2 datacenters, named dc1 and dc2. the gateways of dc1 is 00:00:00:00:00:00:00:02 and 00:00:00:00:00:00:00:03. The gateway of dc2 
      is 00:00:00:00:00:00:00:04, then the registration would be like this:

        curl -d '{"dc1":["00:00:00:00:00:00:00:02", "00:00:00:00:00:00:00:03"], "dc2":["00:00:00:00:00:00:00:04"] }' http://localhost:8080/wm/hadooptopology/gateway/json
    3. After these setting up, you can verify them by querying a host, e.g. 10.0.0.1 using: 
 
        curl -s http://localhost:8080/wm/hadooptopology/?host=10.0.0.1
 
     This should return the network location according to your registration. Our patched datacenter aware Hadoop will use a similar API to conmmunicate with the elf service
    
