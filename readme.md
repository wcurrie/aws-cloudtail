## Visualize AWS CloudTrail logs

As a sequence diagram. Using aws-cloudtrail-processing-library to read CloudTrail logs.

### Instructions for tail -f style

Follow the AWS [instructions](http://docs.aws.amazon.com/awscloudtrail/latest/userguide/send-cloudtrail-events-to-cloudwatch-logs.html) to enable CloudTrail to forward logs CloudWatch Logs.
You can use the watch-cloudtrail.template cloudformation template to create the log group and role. The use to update the trail (created manually).

    aws cloudtrail update-trail --name $trail_name --cloud-watch-logs-log-group-arn $log_group_arn --cloud-watch-logs-role-arn $role_arn

If you prefer to do enable cloudtrail to cloudwatch using terraform, threatstack have a [post](https://blog.threatstack.com/incorporating-aws-security-best-practices-into-terraform-design) showing how.    
    
Run main() in CloudWatchRead.kt. From intellij or whatever.

### config.properties

Should look like:

    bucket_name=<name of bucket cloudtrail is writing logs to>
    key_prefix=Eg: AWSLogs/123456789012/CloudTrail/us-west-2/2017/02/01/
    exclusion_regex=Eg: SomeService|10\\.0\\.0\\.42

### TODO

* handle pagination reading cloudwatch events responses
* mouseover/click to show full event payload