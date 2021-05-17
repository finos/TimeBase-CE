// =============================================================================
// Copyright 2021 EPAM Systems, Inc
//
// See the NOTICE file distributed with this work for additional information
// regarding copyright ownership. Licensed under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// License for the specific language governing permissions and limitations under
// =============================================================================
using EPAM.Deltix.Timebase.Api;
using EPAM.Deltix.Timebase.Api.Schema.Types;
using System;

namespace TimebaseSample
{
    [Deltix.Timebase.Api.SchemaElement(Name = "hubstaff.Activity")]
    public class Activity : Deltix.Timebase.Api.Messages.InstrumentMessage
    {
        [SchemaType(IsNullable = true, DataType = SchemaDataType.Boolean)]
        public sbyte ClientInvoiced { get; set; } = BooleanDataType.Null;
        public long TaskId { get; set; } = IntegerDataType.Int64Null;
        public long Tracked { get; set; } = IntegerDataType.Int64Null;
        [SchemaType(IsNullable = true, DataType = SchemaDataType.Boolean)]
        public sbyte Immutable { get; set; } = BooleanDataType.Null;
        public long UserId { get; set; } = IntegerDataType.Int64Null;
        public long TimesheetId { get; set; } = IntegerDataType.Int64Null;
        public string TimeType { get; set; }
        public string Client { get; set; }
        public long Keyboard { get; set; } = IntegerDataType.Int64Null;
        public DateTime Date { get; set; } = DateTimeDataType.Null;
        public long Overall { get; set; } = IntegerDataType.Int64Null;
        public long Mouse { get; set; } = IntegerDataType.Int64Null;
        [SchemaType(IsNullable = true, DataType = SchemaDataType.Boolean)]
        public sbyte TeamInvoiced { get; set; } = BooleanDataType.Null;
        public long Id { get; set; } = IntegerDataType.Int64Null;
        [SchemaType(IsNullable = true, DataType = SchemaDataType.Boolean)]
        public sbyte Paid { get; set; } = BooleanDataType.Null;
        [SchemaType(IsNullable = true, DataType = SchemaDataType.Boolean)]
        public sbyte TimesheetLocked { get; set; } = BooleanDataType.Null;
        public DateTime TimeSlot { get; set; } = DateTimeDataType.Null;
        public long ProjectId { get; set; } = IntegerDataType.Int64Null;
        public DateTime StartsAt { get; set; } = DateTimeDataType.Null;
        public int Partition { get; set; } = IntegerDataType.Int32Null;
        public long Offset { get; set; } = IntegerDataType.Int64Null;
        public DateTime OriginalTimestamp { get; set; } = DateTimeDataType.Null;

        protected override Deltix.Timebase.Api.Messages.IRecordInterface CopyFromImpl(Deltix.Timebase.Api.Messages.IRecordInfo source)
        {
            base.CopyFrom(source);
            if ((source) is Activity)
            {
                Activity obj = (Activity)source;
                ClientInvoiced = obj.ClientInvoiced;
                TaskId = obj.TaskId;
                Tracked = obj.Tracked;
                Immutable = obj.Immutable;
                UserId = obj.UserId;
                TimesheetId = obj.TimesheetId;
                TimeType = obj.TimeType;
                Client = obj.Client;
                Keyboard = obj.Keyboard;
                Date = obj.Date;
                Overall = obj.Overall;
                Mouse = obj.Mouse;
                TeamInvoiced = obj.TeamInvoiced;
                Id = obj.Id;
                Paid = obj.Paid;
                TimesheetLocked = obj.TimesheetLocked;
                TimeSlot = obj.TimeSlot;
                ProjectId = obj.ProjectId;
                StartsAt = obj.StartsAt;
                Partition = obj.Partition;
                Offset = obj.Offset;
                OriginalTimestamp = obj.OriginalTimestamp;
            }
            return this;
        }
        public override string ToString()
        {
            return base.ToString() + ", " + "client_invoiced" + ": " + ClientInvoiced + ", " + "task_id" + ": " + TaskId + ", " + "tracked" + ": " + Tracked + ", " + "immutable" + ": " + Immutable + ", " + "user_id" + ": " + UserId + ", " + "timesheet_id" + ": " + TimesheetId + ", " + "time_type" + ": " + TimeType + ", " + "client" + ": " + Client + ", " + "keyboard" + ": " + Keyboard + ", " + "date" + ": " + Date + ", " + "overall" + ": " + Overall + ", " + "mouse" + ": " + Mouse + ", " + "team_invoiced" + ": " + TeamInvoiced + ", " + "id" + ": " + Id + ", " + "paid" + ": " + Paid + ", " + "timesheet_locked" + ": " + TimesheetLocked + ", " + "time_slot" + ": " + TimeSlot + ", " + "project_id" + ": " + ProjectId + ", " + "starts_at" + ": " + StartsAt + ", " + "partition" + ": " + Partition + ", " + "offset" + ": " + Offset + ", " + "originalTimestamp" + ": " + OriginalTimestamp;
        }
        protected override Deltix.Timebase.Api.Messages.IRecordInfo CloneImpl()
        {
            Activity msg = new Activity();
            msg.CopyFrom(this);
            return msg;
        }
    }
}