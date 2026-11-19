package com.chitsa.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PersistenceConstant {

    public static final String DATA_CONFLICT =
            "Operation conflicts with existing data. Delete dependent records (such as statements) first.";

    public static final String VERSION_CONFLICT =
            "This record was updated by someone else. Refresh and try again.";
}
