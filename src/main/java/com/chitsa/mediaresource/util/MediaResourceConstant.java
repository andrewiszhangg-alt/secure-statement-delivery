package com.chitsa.mediaresource.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MediaResourceConstant {

    public static final String STORAGE_UNAVAILABLE = "File storage is unavailable, please try again";

    public static final String UPDATE_NOT_SUPPORTED = "MediaResource update is not supported";

    public static final String REQUEST_NOT_NULL = "request must not be null";

    public static final String FILE_EMPTY = "multipartFile must not be empty";

    public static final String FILENAME_BLANK = "originalFilename must not be blank";

    public static final String NO_EXTENSION = "Invalid file: no extension found";

    public static final String CONTENT_TYPE_NOT_ALLOWED = "Content type not allowed: %s. Allowed: %s";

    public static final String EXTENSION_NOT_ALLOWED = "%s cannot be saved. Allowed extensions: %s";

    public static final String FILE_NOT_NULL = "multipartFile must not be null";

    public static final String BUCKET_BLANK = "bucket must not be blank";

    public static final String OBJECT_KEY_BLANK = "objectKey must not be blank";

    public static final String DUPLICATE_OBJECT_KEY = "objectKey already exists: ";

    public static final String NOT_FOUND = "Media resource not found: ";

    public static final String NOT_FOUND_FOR_KEY = "Resource not found for key: ";

    public static final String MEDIA_ID_NOT_NULL = "mediaId must not be null";

    public static final String SIGNATURE_MISMATCH = "File contents do not match the declared type";
}
