package com.epam.deltix.ramdisk;

import com.epam.deltix.util.collections.QuickList;

final class PageContainer extends QuickList.Entry<PageContainer> {
    Page              checkedOutPage;
}