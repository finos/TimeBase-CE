package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *
 */
public interface EntityFilter {
    public static final EntityFilter    ALL = 
        new EntityFilter () {
            @Override
            public boolean      acceptAll () {
                return (true);
            }

            @Override
            public boolean      accept (int entity) {
                return (true);
            }

            @Override
            public boolean restrictAll() {
                return false;
            }

            @Override
            public long         acceptFrom(int entity) {
                return Long.MIN_VALUE;
            }
        };

    public static final EntityFilter    NONE =
            new EntityFilter () {
                @Override
                public boolean      acceptAll () {
                    return (false);
                }

                @Override
                public boolean      accept (int entity) {
                    return (false);
                }

                @Override
                public boolean      restrictAll() {
                    return true;
                }

                @Override
                public long         acceptFrom(int entity) {
                    return Long.MAX_VALUE;
                }
        };
    
    public boolean          acceptAll ();
    
    public boolean          accept (int entity);

    /*
        Returns time from which given entity is accepted.
     */
    public long             acceptFrom (int entity);

    public boolean          restrictAll ();
}
