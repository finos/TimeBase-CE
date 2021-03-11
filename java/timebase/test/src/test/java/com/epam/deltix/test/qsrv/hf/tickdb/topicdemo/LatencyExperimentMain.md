### How to run Topic Latency Experiment


#### Default experiment parameters

Most of measurements were done under this conditions:
* Warm-up period: 20 seconds (parameter `-wt 20`)  
* Experiment duration: 200 seconds (parameter `-lt 200`)  
* Message rate: 20k msg/s (parameter `-mr 20`)
* Guarantied Checkpoint Interval set to 300 seconds (JVM parameter `-XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000`).
Note: Guarantied Safepoint Interval is greater than experiment duration. So guarantied safepoints should not happen during the experiment.


#### Other experiment parameters
* Experiment format: `-ef two` or `-ef simple` - controls experiment format. See JavaDoc for class LatencyExperimentMain.
* Communication type:
  * `-ct topic` for topics
  * `-ct socket` for sockets
* Thread affinity settings: `-af 3,4,5` - lists CPU cores to be used by latency-critical threads.
* Role for current instance:
  * `-mode server` - starts embedded timebase
  * `-mode reader` - starts reader
  * `-mode loader` - starts producer and echo reader
* Server host: `-host localhost`
* Server port: `-port 8011`


#### Environment

To run the experiment from *command line* please setup environment variables:
* `DELTIX_HOME` - as usual
* `TEST_JAVA` - should point to java executable (can be just "java")
* `TEST_JARS` - path to a directory with jar files necessary to run the LatencyExperimentMain class.

You can generate necessary jars by running
```
gradlew :java:cleanSubprojects :java:quantserver:all:gatherAllTestJarsWithDependencies
```

See also `demo_env.bat` and `demo_env.sh` in `scripts` folder. 


#### Execution scripts
Note 1: these scrips assume that you have set environment variables from above.
Note 2: "tuned" scripts assume that the machine has at least 8 CPU cores and cores 2-7 can be used for the experiment.

Each set of startup scripts are assumed to be run in order. Also it's assumed that server will be restarted for each experiment.

All scripts work with topics. To run test with sockets use `-ct socket` instead of `-ct topic`

To isolate CPU cores on CentOS from other user level processes you can use `tuna` tool:
```
sudo tuna --cpus=2,3,4,5,6,7 --isolate
```
Revert back:
```
sudo tuna --cpus=2,3,4,5,6,7 --include
```

##### Round Trip
```bash
"$TEST_JAVA" -cp "$TEST_JARS" deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode server -ef two
"$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode reader -ct topic -ef two
"$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 -DmarkedFraction=1 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode loader -mr 20 -lt 200 -ct topic -gen nanos-yield -ef two
```

##### One Way
```bash
"$TEST_JAVA" -cp "$TEST_JARS" deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode server -ef simple
"$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 -DmarkedFraction=1 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode loader -mr 20 -lt 200 -ct topic -gen nanos-yield -ef simple
```

##### Round Trip (Tuned)
```bash
taskset 0xC0 "$TEST_JAVA" -cp "$TEST_JARS" deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode server -ef two
taskset 0x0C "$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode reader -ct topic -ef two -af 3,2
taskset 0xF0 "$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 -DmarkedFraction=1 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode loader -mr 20 -lt 200 -ct topic -gen nanos-yield -ef two -af 4,5
```

##### One Way (Tuned)
```bash
taskset 0xC0 "$TEST_JAVA" -cp "$TEST_JARS" deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode server -ef simple
taskset 0xF0 "$TEST_JAVA" -cp "$TEST_JARS" -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=300000 -DmarkedFraction=1 deltix.qsrv.hf.tickdb.topicdemo.DemoMain -mode loader -mr 20 -lt 200 -ct topic -gen nanos-yield -ef simple -af 4,5
```