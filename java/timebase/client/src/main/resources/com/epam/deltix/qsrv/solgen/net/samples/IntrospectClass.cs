using EPAM.Deltix.Timebase.Api;
using EPAM.Deltix.Timebase.Api.Schema;
using EPAM.Deltix.Timebase.Api.Utilities.Schema;
using EPAM.Deltix.Timebase.Client;
using System;
using System.Collections.Generic;
using System.Text;

namespace TimebaseSample
{
    class IntrospectClass
    {
        public static void Run()
        {
            using (ITickDb db = TickDbFactory.CreateFromUrl("${timebase.url}"))
            {
                db.Open(false);

                const string streamKey = "${timebase.stream}";

                ITickStream stream = db.GetStream(streamKey);
                if (stream != null)
                    stream.Delete();

                stream = db.CreateStream(streamKey, streamKey, null, 0);

                Introspector instrospector = Introspector.CreateMessageIntrospector();
                RecordClassDescriptor descriptor = instrospector.IntrospectRecordClass(typeof(Activity));

                stream.SetFixedType(descriptor);
            }
        }
    }
}
