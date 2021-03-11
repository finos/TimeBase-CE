call demo_env.bat
"%TEST_JAVA%" -cp "%TEST_JARS%" -DmarkedFraction=1 deltix.qsrv.hf.tickdb.topicdemo.LatencyExperimentMain -mode loader -mr 20 -lt 200 -ct topic -gen nanos-yield