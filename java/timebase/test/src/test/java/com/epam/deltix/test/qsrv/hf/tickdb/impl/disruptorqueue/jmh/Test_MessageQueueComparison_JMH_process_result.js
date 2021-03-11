/**
 * This file is utility script for post-processing JSON output of Test_MessageQueueComparison_JMH.
 *
 * Usage:
 * 1. Run Test_MessageQueueComparison_JMH with JSON output format option.
 * 2. Output resulting message_queue_measurement_report.json file, copy it's content and paste it into variable "x" in this script.
 * 3. Run this script with Browser or NodeJs.
 */

function groupData(rawReport) {
    function getFilteredParams(params) {
        var result = {};
        for (var key in params) {
            if (params.hasOwnProperty(key) && key != 'disruptor') {
                result[key] = params[key];
            }
        }
        return result;
    }

    var out = {};
    for (var i = 0; i < rawReport.length; i++) {
        var bench = rawReport[i];
        var fullName = bench.benchmark;
        var nameParts = fullName.split('.');
        var name = nameParts[nameParts.length - 1].replace('testThroughput', '');

        var params = bench.params;
        var lossless = params.lossless;
        var remote = params.remote;
        var queueSizeKb = params.queueSizeKb;
        var key = name + "_" + remote + "_" + lossless + "_" + queueSizeKb;

        if (!out.hasOwnProperty(key)) {
            out[key] = {
                "name": name,
                "params": getFilteredParams(params)
            }
        }
        //var value = bench.score;
        var secondaryMetrics = bench.secondaryMetrics;
        var consumerStats, producerStats;
        for (var secondaryMetricName in secondaryMetrics) {
            if (secondaryMetrics.hasOwnProperty(secondaryMetricName)) {
                if (secondaryMetricName.indexOf('Consumer') >= 0) {
                    consumerStats = secondaryMetrics[secondaryMetricName];
                } else if (secondaryMetricName.indexOf('Producer') >= 0) {
                    producerStats = secondaryMetrics[secondaryMetricName];
                }
            }
        }

        var queueName = params.disruptor == "true" ? 'disruptor' : 'old';
        out[key][queueName] = {
            "producer": producerStats,
            "consumer": consumerStats
        }
    }
    return out;
}

function buildReport(groupedRecords) {
    var result = [];
    for (var key in groupedRecords) {
        if (groupedRecords.hasOwnProperty(key)) {
            var record = groupedRecords[key];
            result.push([
                record.params.lossless,
                record.params.remote,
                record.params.queueSizeKb,
                record.name,
                record.name + (record.params.lossless == "true" ? '-LOSSLESS' : '-LOSSY') + (record.params.remote == "true" ? '-R' : '-L'),
                record.old ? Math.round(record.old.consumer.score) : '',
                record.disruptor ? Math.round(record.disruptor.consumer.score) : '',
                '',
                record.old ? Math.round(record.old.producer.score) : '',
                record.disruptor ? Math.round(record.disruptor.producer.score) : ''
            ].join('\t'));
        }
    }
    return result.join('\n');
}

// Place report data here (replace empty array with report's array).
var x = [
];


var report = buildReport(groupData(x));
console.log(report);