
## Instructions

Save Athena JDBC driver to lib/

    wget https://s3.amazonaws.com/athena-downloads/drivers/AthenaJDBC41-1.0.0.jar
    mvn install:install-file -Dfile=AthenaJDBC41-1.0.0.jar -DgroupId=aws.local \
        -DartifactId=athena-jdbc-driver -Dversion=1.0.0 -Dpackaging=jar

## Resources

* CloudTrail + Athena [blog post](https://aws.amazon.com/blogs/big-data/aws-cloudtrail-and-amazon-athena-dive-deep-to-analyze-security-compliance-and-operational-activity/)
* [Athena JDBC Driver](http://docs.aws.amazon.com/athena/latest/ug/connect-with-jdbc.html)
* Pre-Athena [post](http://blog.fzakaria.com/2014/10/13/analyzing-cloudtrail-logs-using-hivehadoop/) using EMR instead.
* [post](http://aws.mannem.me/?p=1366) using AWS EMR
* Using Apache Spark in 2015 [timely-security-analytics](https://github.com/awslabs/timely-security-analytics)
* https://thomasvachon.com/articles/using-aws-athena-to-query-cloudtrail-logs/
* Hive SerDe https://cwiki.apache.org/confluence/display/Hive/SerDe