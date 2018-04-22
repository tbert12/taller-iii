@echo off
rmdir /s /q err\ 2>nul
rmdir /s /q err_temp\ 2>nul
del /s /q apache_dump.log yaam.log apache_error.log error_frequency.log 2>nul

REM Test Case:
REM     1. Errors Frequency (only with `error_log`)
REM         - Work (DEBUG Activate for sleep in reader)
REM             9 errors of 2003-november.txt
REM         - Volume
REM     2. Dumpers errors and log_file (same line count)
REM     3. Volume with task admin.


FOR /L %%G IN (1,1,1) do ( type enunciado\Apache-logs-samples\log ) >> test_log.temp
REM                ^-- number of copies
type test_log.temp | java -jar out\artifacts\TP0_ApacheLogger_jar\TP0-ApacheLogger.jar
del /s/q test_log.temp