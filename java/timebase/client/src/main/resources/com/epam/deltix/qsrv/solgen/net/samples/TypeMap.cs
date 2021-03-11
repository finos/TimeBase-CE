using EPAM.Deltix.Timebase.Api.Utilities.Binding;

namespace TimebaseSample
{

    public class TypeMap
    {
        public static readonly TypeLoader TYPE_LOADER = new TypeLoader();

        static TypeMap()
        {
            TYPE_LOADER.AddType(typeof(Activity));
        }
    }
}

