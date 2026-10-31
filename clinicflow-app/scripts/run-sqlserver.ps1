$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"
$env:PATH = "$env:JAVA_HOME\bin;D:\JAVA_APP\PROJECT\tools\apache-maven-3.9.11\bin;$env:PATH"
Set-Location "D:\JAVA_APP\PROJECT\clinicflow-app"
mvn -B spring-boot:run
