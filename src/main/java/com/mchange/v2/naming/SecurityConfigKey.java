package com.mchange.v2.naming;

public class SecurityConfigKey {

    public final static String SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION = "com.mchange.v2.naming.supportReferenceRemoteFactoryClassLocation";

    public final static String NAME_GUARD_CLASS_NAME = "com.mchange.v2.naming.nameGuardClassName";

    // used in ReferenceableUtils.referenceToObject when no whitelist is provided explicitly
    public final static String OBJECT_FACTORY_WHITELIST = "com.mchange.v2.naming.objectFactoryWhitelist";

    public final static String ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT = "com.mchange.v2.naming.acceptDeserializedInitialContextEnvironment";

    public final static String ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE = "com.mchange.v2.naming.allowIndirectSerializationViaReference";

    public final static String GENERATE_SERIALIZED_OBJECT_BINARY_REF_ADDR = "com.mchange.v2.naming.generateSerializedObjectBinaryRefAddr";

    private SecurityConfigKey() {}
}
