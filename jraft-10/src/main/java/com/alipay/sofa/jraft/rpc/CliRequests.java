
package com.alipay.sofa.jraft.rpc;

public final class CliRequests {
  private CliRequests() {
  }

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public interface AddPeerRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.AddPeerRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>required string peer_id = 3;</code>
     */
    boolean hasPeerId();

    /**
     * <code>required string peer_id = 3;</code>
     */
    String getPeerId();

    /**
     * <code>required string peer_id = 3;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();
  }

  /**
   * Protobuf type {@code jraft.AddPeerRequest}
   */
  public static final class AddPeerRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.AddPeerRequest)
          AddPeerRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use AddPeerRequest.newBuilder() to construct.
    private AddPeerRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private AddPeerRequest() {
      groupId_ = "";
      leaderId_ = "";
      peerId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private AddPeerRequest(com.google.protobuf.CodedInputStream input,
                           com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000004;
              peerId_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_AddPeerRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_AddPeerRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(AddPeerRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 3;
    private volatile Object peerId_;

    /**
     * <code>required string peer_id = 3;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }

    /**
     * <code>required string peer_id = 3;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string peer_id = 3;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasPeerId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, peerId_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, peerId_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof AddPeerRequest)) {
        return super.equals(obj);
      }
      AddPeerRequest other = (AddPeerRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static AddPeerRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerRequest parseFrom(java.nio.ByteBuffer data,
                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerRequest parseFrom(byte[] data,
                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddPeerRequest parseFrom(java.io.InputStream input,
                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static AddPeerRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static AddPeerRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static AddPeerRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddPeerRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(AddPeerRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.AddPeerRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.AddPeerRequest)
            AddPeerRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_AddPeerRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_AddPeerRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(AddPeerRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.AddPeerRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_AddPeerRequest_descriptor;
      }

      public AddPeerRequest getDefaultInstanceForType() {
        return AddPeerRequest.getDefaultInstance();
      }

      public AddPeerRequest build() {
        AddPeerRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public AddPeerRequest buildPartial() {
        AddPeerRequest result = new AddPeerRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.peerId_ = peerId_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof AddPeerRequest) {
          return mergeFrom((AddPeerRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(AddPeerRequest other) {
        if (other == AddPeerRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000004;
          peerId_ = other.peerId_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        if (!hasPeerId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        AddPeerRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (AddPeerRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>required string peer_id = 3;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000004);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.AddPeerRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.AddPeerRequest)
    private static final AddPeerRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new AddPeerRequest();
    }

    public static AddPeerRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<AddPeerRequest> PARSER = new com.google.protobuf.AbstractParser<AddPeerRequest>() {
      public AddPeerRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new AddPeerRequest(input,
                extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<AddPeerRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<AddPeerRequest> getParserForType() {
      return PARSER;
    }

    public AddPeerRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface AddPeerResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.AddPeerResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    java.util.List<String> getOldPeersList();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    int getOldPeersCount();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    String getOldPeers(int index);

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    com.google.protobuf.ByteString getOldPeersBytes(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    java.util.List<String> getNewPeersList();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    int getNewPeersCount();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    String getNewPeers(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    com.google.protobuf.ByteString getNewPeersBytes(int index);

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.AddPeerResponse}
   */
  public static final class AddPeerResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.AddPeerResponse)
          AddPeerResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use AddPeerResponse.newBuilder() to construct.
    private AddPeerResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private AddPeerResponse() {
      oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private AddPeerResponse(com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                oldPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              oldPeers_.add(bs);
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                newPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              newPeers_.add(bs);
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_AddPeerResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_AddPeerResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(AddPeerResponse.class,
                      Builder.class);
    }

    private int                                bitField0_;
    public static final int                    OLD_PEERS_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList oldPeers_;

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList getOldPeersList() {
      return oldPeers_;
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public int getOldPeersCount() {
      return oldPeers_.size();
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public String getOldPeers(int index) {
      return oldPeers_.get(index);
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ByteString getOldPeersBytes(int index) {
      return oldPeers_.getByteString(index);
    }

    public static final int                    NEW_PEERS_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList newPeers_;

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewPeersList() {
      return newPeers_;
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public int getNewPeersCount() {
      return newPeers_.size();
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public String getNewPeers(int index) {
      return newPeers_.get(index);
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ByteString getNewPeersBytes(int index) {
      return newPeers_.getByteString(index);
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < oldPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, oldPeers_.getRaw(i));
      }
      for (int i = 0; i < newPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, newPeers_.getRaw(i));
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < oldPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(oldPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getOldPeersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(newPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewPeersList().size();
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof AddPeerResponse)) {
        return super.equals(obj);
      }
      AddPeerResponse other = (AddPeerResponse) obj;

      boolean result = true;
      result = result && getOldPeersList().equals(other.getOldPeersList());
      result = result && getNewPeersList().equals(other.getNewPeersList());
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getOldPeersCount() > 0) {
        hash = (37 * hash) + OLD_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getOldPeersList().hashCode();
      }
      if (getNewPeersCount() > 0) {
        hash = (37 * hash) + NEW_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewPeersList().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static AddPeerResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerResponse parseFrom(java.nio.ByteBuffer data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddPeerResponse parseFrom(byte[] data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddPeerResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddPeerResponse parseFrom(java.io.InputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static AddPeerResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static AddPeerResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                           com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static AddPeerResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddPeerResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(AddPeerResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.AddPeerResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.AddPeerResponse)
            AddPeerResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_AddPeerResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_AddPeerResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(AddPeerResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.AddPeerResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_AddPeerResponse_descriptor;
      }

      public AddPeerResponse getDefaultInstanceForType() {
        return AddPeerResponse.getDefaultInstance();
      }

      public AddPeerResponse build() {
        AddPeerResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public AddPeerResponse buildPartial() {
        AddPeerResponse result = new AddPeerResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.oldPeers_ = oldPeers_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.newPeers_ = newPeers_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000001;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof AddPeerResponse) {
          return mergeFrom((AddPeerResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(AddPeerResponse other) {
        if (other == AddPeerResponse.getDefaultInstance())
          return this;
        if (!other.oldPeers_.isEmpty()) {
          if (oldPeers_.isEmpty()) {
            oldPeers_ = other.oldPeers_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureOldPeersIsMutable();
            oldPeers_.addAll(other.oldPeers_);
          }
          onChanged();
        }
        if (!other.newPeers_.isEmpty()) {
          if (newPeers_.isEmpty()) {
            newPeers_ = other.newPeers_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureNewPeersIsMutable();
            newPeers_.addAll(other.newPeers_);
          }
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        AddPeerResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (AddPeerResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int                                bitField0_;

      private com.google.protobuf.LazyStringList oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureOldPeersIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = new com.google.protobuf.LazyStringArrayList(oldPeers_);
          bitField0_ |= 0x00000001;
        }
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList getOldPeersList() {
        return oldPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public int getOldPeersCount() {
        return oldPeers_.size();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public String getOldPeers(int index) {
        return oldPeers_.get(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ByteString getOldPeersBytes(int index) {
        return oldPeers_.getByteString(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder setOldPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addAllOldPeers(Iterable<String> values) {
        ensureOldPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, oldPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder clearOldPeers() {
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewPeersIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = new com.google.protobuf.LazyStringArrayList(newPeers_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewPeersList() {
        return newPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public int getNewPeersCount() {
        return newPeers_.size();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public String getNewPeers(int index) {
        return newPeers_.get(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ByteString getNewPeersBytes(int index) {
        return newPeers_.getByteString(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder setNewPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addAllNewPeers(Iterable<String> values) {
        ensureNewPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder clearNewPeers() {
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.AddPeerResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.AddPeerResponse)
    private static final AddPeerResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new AddPeerResponse();
    }

    public static AddPeerResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<AddPeerResponse> PARSER = new com.google.protobuf.AbstractParser<AddPeerResponse>() {
      public AddPeerResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new AddPeerResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<AddPeerResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<AddPeerResponse> getParserForType() {
      return PARSER;
    }

    public AddPeerResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface RemovePeerRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.RemovePeerRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>required string peer_id = 3;</code>
     */
    boolean hasPeerId();

    /**
     * <code>required string peer_id = 3;</code>
     */
    String getPeerId();

    /**
     * <code>required string peer_id = 3;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();
  }

  /**
   * Protobuf type {@code jraft.RemovePeerRequest}
   */
  public static final class RemovePeerRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.RemovePeerRequest)
          RemovePeerRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use RemovePeerRequest.newBuilder() to construct.
    private RemovePeerRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private RemovePeerRequest() {
      groupId_ = "";
      leaderId_ = "";
      peerId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private RemovePeerRequest(com.google.protobuf.CodedInputStream input,
                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000004;
              peerId_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_RemovePeerRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_RemovePeerRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(RemovePeerRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 3;
    private volatile Object peerId_;

    /**
     * <code>required string peer_id = 3;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }

    /**
     * <code>required string peer_id = 3;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string peer_id = 3;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasPeerId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, peerId_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, peerId_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof RemovePeerRequest)) {
        return super.equals(obj);
      }
      RemovePeerRequest other = (RemovePeerRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static RemovePeerRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerRequest parseFrom(java.nio.ByteBuffer data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerRequest parseFrom(byte[] data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemovePeerRequest parseFrom(java.io.InputStream input,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static RemovePeerRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static RemovePeerRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static RemovePeerRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemovePeerRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(RemovePeerRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.RemovePeerRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.RemovePeerRequest)
            RemovePeerRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_RemovePeerRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_RemovePeerRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(RemovePeerRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.RemovePeerRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_RemovePeerRequest_descriptor;
      }

      public RemovePeerRequest getDefaultInstanceForType() {
        return RemovePeerRequest.getDefaultInstance();
      }

      public RemovePeerRequest build() {
        RemovePeerRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public RemovePeerRequest buildPartial() {
        RemovePeerRequest result = new RemovePeerRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.peerId_ = peerId_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof RemovePeerRequest) {
          return mergeFrom((RemovePeerRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(RemovePeerRequest other) {
        if (other == RemovePeerRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000004;
          peerId_ = other.peerId_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        if (!hasPeerId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        RemovePeerRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (RemovePeerRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>required string peer_id = 3;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000004);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 3;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.RemovePeerRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.RemovePeerRequest)
    private static final RemovePeerRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new RemovePeerRequest();
    }

    public static RemovePeerRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<RemovePeerRequest> PARSER = new com.google.protobuf.AbstractParser<RemovePeerRequest>() {
      public RemovePeerRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new RemovePeerRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<RemovePeerRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<RemovePeerRequest> getParserForType() {
      return PARSER;
    }

    public RemovePeerRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface RemovePeerResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.RemovePeerResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    java.util.List<String> getOldPeersList();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    int getOldPeersCount();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    String getOldPeers(int index);

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    com.google.protobuf.ByteString getOldPeersBytes(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    java.util.List<String> getNewPeersList();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    int getNewPeersCount();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    String getNewPeers(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    com.google.protobuf.ByteString getNewPeersBytes(int index);

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.RemovePeerResponse}
   */
  public static final class RemovePeerResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.RemovePeerResponse)
          RemovePeerResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use RemovePeerResponse.newBuilder() to construct.
    private RemovePeerResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private RemovePeerResponse() {
      oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private RemovePeerResponse(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                oldPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              oldPeers_.add(bs);
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                newPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              newPeers_.add(bs);
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_RemovePeerResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_RemovePeerResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(RemovePeerResponse.class,
                      Builder.class);
    }

    private int                                bitField0_;
    public static final int                    OLD_PEERS_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList oldPeers_;

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList getOldPeersList() {
      return oldPeers_;
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public int getOldPeersCount() {
      return oldPeers_.size();
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public String getOldPeers(int index) {
      return oldPeers_.get(index);
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ByteString getOldPeersBytes(int index) {
      return oldPeers_.getByteString(index);
    }

    public static final int                    NEW_PEERS_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList newPeers_;

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewPeersList() {
      return newPeers_;
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public int getNewPeersCount() {
      return newPeers_.size();
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public String getNewPeers(int index) {
      return newPeers_.get(index);
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ByteString getNewPeersBytes(int index) {
      return newPeers_.getByteString(index);
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < oldPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, oldPeers_.getRaw(i));
      }
      for (int i = 0; i < newPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, newPeers_.getRaw(i));
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < oldPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(oldPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getOldPeersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(newPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewPeersList().size();
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof RemovePeerResponse)) {
        return super.equals(obj);
      }
      RemovePeerResponse other = (RemovePeerResponse) obj;

      boolean result = true;
      result = result && getOldPeersList().equals(other.getOldPeersList());
      result = result && getNewPeersList().equals(other.getNewPeersList());
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getOldPeersCount() > 0) {
        hash = (37 * hash) + OLD_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getOldPeersList().hashCode();
      }
      if (getNewPeersCount() > 0) {
        hash = (37 * hash) + NEW_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewPeersList().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static RemovePeerResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerResponse parseFrom(java.nio.ByteBuffer data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemovePeerResponse parseFrom(byte[] data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemovePeerResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemovePeerResponse parseFrom(java.io.InputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static RemovePeerResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static RemovePeerResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static RemovePeerResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemovePeerResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(RemovePeerResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.RemovePeerResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.RemovePeerResponse)
            RemovePeerResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_RemovePeerResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_RemovePeerResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(RemovePeerResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.RemovePeerResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_RemovePeerResponse_descriptor;
      }

      public RemovePeerResponse getDefaultInstanceForType() {
        return RemovePeerResponse.getDefaultInstance();
      }

      public RemovePeerResponse build() {
        RemovePeerResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public RemovePeerResponse buildPartial() {
        RemovePeerResponse result = new RemovePeerResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.oldPeers_ = oldPeers_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.newPeers_ = newPeers_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000001;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof RemovePeerResponse) {
          return mergeFrom((RemovePeerResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(RemovePeerResponse other) {
        if (other == RemovePeerResponse.getDefaultInstance())
          return this;
        if (!other.oldPeers_.isEmpty()) {
          if (oldPeers_.isEmpty()) {
            oldPeers_ = other.oldPeers_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureOldPeersIsMutable();
            oldPeers_.addAll(other.oldPeers_);
          }
          onChanged();
        }
        if (!other.newPeers_.isEmpty()) {
          if (newPeers_.isEmpty()) {
            newPeers_ = other.newPeers_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureNewPeersIsMutable();
            newPeers_.addAll(other.newPeers_);
          }
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        RemovePeerResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (RemovePeerResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int                                bitField0_;

      private com.google.protobuf.LazyStringList oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureOldPeersIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = new com.google.protobuf.LazyStringArrayList(oldPeers_);
          bitField0_ |= 0x00000001;
        }
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList getOldPeersList() {
        return oldPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public int getOldPeersCount() {
        return oldPeers_.size();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public String getOldPeers(int index) {
        return oldPeers_.get(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ByteString getOldPeersBytes(int index) {
        return oldPeers_.getByteString(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder setOldPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addAllOldPeers(Iterable<String> values) {
        ensureOldPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, oldPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder clearOldPeers() {
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewPeersIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = new com.google.protobuf.LazyStringArrayList(newPeers_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewPeersList() {
        return newPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public int getNewPeersCount() {
        return newPeers_.size();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public String getNewPeers(int index) {
        return newPeers_.get(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ByteString getNewPeersBytes(int index) {
        return newPeers_.getByteString(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder setNewPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addAllNewPeers(Iterable<String> values) {
        ensureNewPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder clearNewPeers() {
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.RemovePeerResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.RemovePeerResponse)
    private static final RemovePeerResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new RemovePeerResponse();
    }

    public static RemovePeerResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<RemovePeerResponse> PARSER = new com.google.protobuf.AbstractParser<RemovePeerResponse>() {
      public RemovePeerResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new RemovePeerResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<RemovePeerResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<RemovePeerResponse> getParserForType() {
      return PARSER;
    }

    public RemovePeerResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface ChangePeersRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.ChangePeersRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    java.util.List<String> getNewPeersList();

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    int getNewPeersCount();

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    String getNewPeers(int index);

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    com.google.protobuf.ByteString getNewPeersBytes(int index);
  }

  /**
   * Protobuf type {@code jraft.ChangePeersRequest}
   */
  public static final class ChangePeersRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.ChangePeersRequest)
          ChangePeersRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ChangePeersRequest.newBuilder() to construct.
    private ChangePeersRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private ChangePeersRequest() {
      groupId_ = "";
      leaderId_ = "";
      newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private ChangePeersRequest(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                newPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              newPeers_.add(bs);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          newPeers_ = newPeers_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_ChangePeersRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_ChangePeersRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(ChangePeersRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                    NEW_PEERS_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList newPeers_;

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewPeersList() {
      return newPeers_;
    }

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    public int getNewPeersCount() {
      return newPeers_.size();
    }

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    public String getNewPeers(int index) {
      return newPeers_.get(index);
    }

    /**
     * <code>repeated string new_peers = 3;</code>
     */
    public com.google.protobuf.ByteString getNewPeersBytes(int index) {
      return newPeers_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      for (int i = 0; i < newPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, newPeers_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(newPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewPeersList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ChangePeersRequest)) {
        return super.equals(obj);
      }
      ChangePeersRequest other = (ChangePeersRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && getNewPeersList().equals(other.getNewPeersList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (getNewPeersCount() > 0) {
        hash = (37 * hash) + NEW_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewPeersList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static ChangePeersRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersRequest parseFrom(java.nio.ByteBuffer data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersRequest parseFrom(byte[] data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ChangePeersRequest parseFrom(java.io.InputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ChangePeersRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ChangePeersRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static ChangePeersRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ChangePeersRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ChangePeersRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.ChangePeersRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.ChangePeersRequest)
            ChangePeersRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_ChangePeersRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_ChangePeersRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(ChangePeersRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.ChangePeersRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_ChangePeersRequest_descriptor;
      }

      public ChangePeersRequest getDefaultInstanceForType() {
        return ChangePeersRequest.getDefaultInstance();
      }

      public ChangePeersRequest build() {
        ChangePeersRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ChangePeersRequest buildPartial() {
        ChangePeersRequest result = new ChangePeersRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          newPeers_ = newPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.newPeers_ = newPeers_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ChangePeersRequest) {
          return mergeFrom((ChangePeersRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ChangePeersRequest other) {
        if (other == ChangePeersRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (!other.newPeers_.isEmpty()) {
          if (newPeers_.isEmpty()) {
            newPeers_ = other.newPeers_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureNewPeersIsMutable();
            newPeers_.addAll(other.newPeers_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        ChangePeersRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ChangePeersRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewPeersIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          newPeers_ = new com.google.protobuf.LazyStringArrayList(newPeers_);
          bitField0_ |= 0x00000004;
        }
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewPeersList() {
        return newPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public int getNewPeersCount() {
        return newPeers_.size();
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public String getNewPeers(int index) {
        return newPeers_.get(index);
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public com.google.protobuf.ByteString getNewPeersBytes(int index) {
        return newPeers_.getByteString(index);
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public Builder setNewPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public Builder addNewPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public Builder addAllNewPeers(Iterable<String> values) {
        ensureNewPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public Builder clearNewPeers() {
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 3;</code>
       */
      public Builder addNewPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.ChangePeersRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.ChangePeersRequest)
    private static final ChangePeersRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new ChangePeersRequest();
    }

    public static ChangePeersRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<ChangePeersRequest> PARSER = new com.google.protobuf.AbstractParser<ChangePeersRequest>() {
      public ChangePeersRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new ChangePeersRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<ChangePeersRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ChangePeersRequest> getParserForType() {
      return PARSER;
    }

    public ChangePeersRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface ChangePeersResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.ChangePeersResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    java.util.List<String> getOldPeersList();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    int getOldPeersCount();

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    String getOldPeers(int index);

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    com.google.protobuf.ByteString getOldPeersBytes(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    java.util.List<String> getNewPeersList();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    int getNewPeersCount();

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    String getNewPeers(int index);

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    com.google.protobuf.ByteString getNewPeersBytes(int index);

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.ChangePeersResponse}
   */
  public static final class ChangePeersResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.ChangePeersResponse)
          ChangePeersResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ChangePeersResponse.newBuilder() to construct.
    private ChangePeersResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private ChangePeersResponse() {
      oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private ChangePeersResponse(com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                oldPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              oldPeers_.add(bs);
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                newPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              newPeers_.add(bs);
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_ChangePeersResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_ChangePeersResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(ChangePeersResponse.class,
                      Builder.class);
    }

    private int                                bitField0_;
    public static final int                    OLD_PEERS_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList oldPeers_;

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList getOldPeersList() {
      return oldPeers_;
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public int getOldPeersCount() {
      return oldPeers_.size();
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public String getOldPeers(int index) {
      return oldPeers_.get(index);
    }

    /**
     * <code>repeated string old_peers = 1;</code>
     */
    public com.google.protobuf.ByteString getOldPeersBytes(int index) {
      return oldPeers_.getByteString(index);
    }

    public static final int                    NEW_PEERS_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList newPeers_;

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewPeersList() {
      return newPeers_;
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public int getNewPeersCount() {
      return newPeers_.size();
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public String getNewPeers(int index) {
      return newPeers_.get(index);
    }

    /**
     * <code>repeated string new_peers = 2;</code>
     */
    public com.google.protobuf.ByteString getNewPeersBytes(int index) {
      return newPeers_.getByteString(index);
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < oldPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, oldPeers_.getRaw(i));
      }
      for (int i = 0; i < newPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, newPeers_.getRaw(i));
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < oldPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(oldPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getOldPeersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(newPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewPeersList().size();
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ChangePeersResponse)) {
        return super.equals(obj);
      }
      ChangePeersResponse other = (ChangePeersResponse) obj;

      boolean result = true;
      result = result && getOldPeersList().equals(other.getOldPeersList());
      result = result && getNewPeersList().equals(other.getNewPeersList());
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getOldPeersCount() > 0) {
        hash = (37 * hash) + OLD_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getOldPeersList().hashCode();
      }
      if (getNewPeersCount() > 0) {
        hash = (37 * hash) + NEW_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewPeersList().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static ChangePeersResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersResponse parseFrom(java.nio.ByteBuffer data,
                                                                                      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ChangePeersResponse parseFrom(byte[] data,
                                                                                      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ChangePeersResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ChangePeersResponse parseFrom(java.io.InputStream input,
                                                                                      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ChangePeersResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ChangePeersResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static ChangePeersResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ChangePeersResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ChangePeersResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.ChangePeersResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.ChangePeersResponse)
            ChangePeersResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_ChangePeersResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_ChangePeersResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(ChangePeersResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.ChangePeersResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_ChangePeersResponse_descriptor;
      }

      public ChangePeersResponse getDefaultInstanceForType() {
        return ChangePeersResponse.getDefaultInstance();
      }

      public ChangePeersResponse build() {
        ChangePeersResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ChangePeersResponse buildPartial() {
        ChangePeersResponse result = new ChangePeersResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.oldPeers_ = oldPeers_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = newPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.newPeers_ = newPeers_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000001;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ChangePeersResponse) {
          return mergeFrom((ChangePeersResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ChangePeersResponse other) {
        if (other == ChangePeersResponse.getDefaultInstance())
          return this;
        if (!other.oldPeers_.isEmpty()) {
          if (oldPeers_.isEmpty()) {
            oldPeers_ = other.oldPeers_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureOldPeersIsMutable();
            oldPeers_.addAll(other.oldPeers_);
          }
          onChanged();
        }
        if (!other.newPeers_.isEmpty()) {
          if (newPeers_.isEmpty()) {
            newPeers_ = other.newPeers_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureNewPeersIsMutable();
            newPeers_.addAll(other.newPeers_);
          }
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        ChangePeersResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ChangePeersResponse) e
                  .getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int                                bitField0_;

      private com.google.protobuf.LazyStringList oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureOldPeersIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          oldPeers_ = new com.google.protobuf.LazyStringArrayList(oldPeers_);
          bitField0_ |= 0x00000001;
        }
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList getOldPeersList() {
        return oldPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public int getOldPeersCount() {
        return oldPeers_.size();
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public String getOldPeers(int index) {
        return oldPeers_.get(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public com.google.protobuf.ByteString getOldPeersBytes(int index) {
        return oldPeers_.getByteString(index);
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder setOldPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addAllOldPeers(Iterable<String> values) {
        ensureOldPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, oldPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder clearOldPeers() {
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 1;</code>
       */
      public Builder addOldPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewPeersIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          newPeers_ = new com.google.protobuf.LazyStringArrayList(newPeers_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewPeersList() {
        return newPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public int getNewPeersCount() {
        return newPeers_.size();
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public String getNewPeers(int index) {
        return newPeers_.get(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public com.google.protobuf.ByteString getNewPeersBytes(int index) {
        return newPeers_.getByteString(index);
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder setNewPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addAllNewPeers(Iterable<String> values) {
        ensureNewPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder clearNewPeers() {
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 2;</code>
       */
      public Builder addNewPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.ChangePeersResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.ChangePeersResponse)
    private static final ChangePeersResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new ChangePeersResponse();
    }

    public static ChangePeersResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<ChangePeersResponse> PARSER = new com.google.protobuf.AbstractParser<ChangePeersResponse>() {
      public ChangePeersResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new ChangePeersResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<ChangePeersResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ChangePeersResponse> getParserForType() {
      return PARSER;
    }

    public ChangePeersResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface SnapshotRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.SnapshotRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    boolean hasPeerId();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    String getPeerId();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();
  }

  /**
   * Protobuf type {@code jraft.SnapshotRequest}
   */
  public static final class SnapshotRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.SnapshotRequest)
          SnapshotRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use SnapshotRequest.newBuilder() to construct.
    private SnapshotRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private SnapshotRequest() {
      groupId_ = "";
      peerId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private SnapshotRequest(com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              peerId_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_SnapshotRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_SnapshotRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(SnapshotRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 2;
    private volatile Object peerId_;

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, peerId_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, peerId_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SnapshotRequest)) {
        return super.equals(obj);
      }
      SnapshotRequest other = (SnapshotRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static SnapshotRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static SnapshotRequest parseFrom(java.nio.ByteBuffer data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static SnapshotRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static SnapshotRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static SnapshotRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static SnapshotRequest parseFrom(byte[] data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static SnapshotRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static SnapshotRequest parseFrom(java.io.InputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static SnapshotRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static SnapshotRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                           com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static SnapshotRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static SnapshotRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(SnapshotRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.SnapshotRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.SnapshotRequest)
            SnapshotRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_SnapshotRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_SnapshotRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(SnapshotRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.SnapshotRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_SnapshotRequest_descriptor;
      }

      public SnapshotRequest getDefaultInstanceForType() {
        return SnapshotRequest.getDefaultInstance();
      }

      public SnapshotRequest build() {
        SnapshotRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public SnapshotRequest buildPartial() {
        SnapshotRequest result = new SnapshotRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.peerId_ = peerId_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof SnapshotRequest) {
          return mergeFrom((SnapshotRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(SnapshotRequest other) {
        if (other == SnapshotRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000002;
          peerId_ = other.peerId_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        SnapshotRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (SnapshotRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.SnapshotRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.SnapshotRequest)
    private static final SnapshotRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new SnapshotRequest();
    }

    public static SnapshotRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<SnapshotRequest> PARSER = new com.google.protobuf.AbstractParser<SnapshotRequest>() {
      public SnapshotRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new SnapshotRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<SnapshotRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<SnapshotRequest> getParserForType() {
      return PARSER;
    }

    public SnapshotRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface ResetPeerRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.ResetPeerRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string peer_id = 2;</code>
     */
    boolean hasPeerId();

    /**
     * <code>required string peer_id = 2;</code>
     */
    String getPeerId();

    /**
     * <code>required string peer_id = 2;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    java.util.List<String> getOldPeersList();

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    int getOldPeersCount();

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    String getOldPeers(int index);

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    com.google.protobuf.ByteString getOldPeersBytes(int index);

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    java.util.List<String> getNewPeersList();

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    int getNewPeersCount();

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    String getNewPeers(int index);

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    com.google.protobuf.ByteString getNewPeersBytes(int index);
  }

  /**
   * Protobuf type {@code jraft.ResetPeerRequest}
   */
  public static final class ResetPeerRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.ResetPeerRequest)
          ResetPeerRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ResetPeerRequest.newBuilder() to construct.
    private ResetPeerRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private ResetPeerRequest() {
      groupId_ = "";
      peerId_ = "";
      oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private ResetPeerRequest(com.google.protobuf.CodedInputStream input,
                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              peerId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                oldPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              oldPeers_.add(bs);
              break;
            }
            case 34: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
                newPeers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000008;
              }
              newPeers_.add(bs);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
          newPeers_ = newPeers_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_ResetPeerRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_ResetPeerRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(ResetPeerRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 2;
    private volatile Object peerId_;

    /**
     * <code>required string peer_id = 2;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string peer_id = 2;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string peer_id = 2;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                    OLD_PEERS_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList oldPeers_;

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList getOldPeersList() {
      return oldPeers_;
    }

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    public int getOldPeersCount() {
      return oldPeers_.size();
    }

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    public String getOldPeers(int index) {
      return oldPeers_.get(index);
    }

    /**
     * <code>repeated string old_peers = 3;</code>
     */
    public com.google.protobuf.ByteString getOldPeersBytes(int index) {
      return oldPeers_.getByteString(index);
    }

    public static final int                    NEW_PEERS_FIELD_NUMBER = 4;
    private com.google.protobuf.LazyStringList newPeers_;

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewPeersList() {
      return newPeers_;
    }

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    public int getNewPeersCount() {
      return newPeers_.size();
    }

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    public String getNewPeers(int index) {
      return newPeers_.get(index);
    }

    /**
     * <code>repeated string new_peers = 4;</code>
     */
    public com.google.protobuf.ByteString getNewPeersBytes(int index) {
      return newPeers_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasPeerId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, peerId_);
      }
      for (int i = 0; i < oldPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, oldPeers_.getRaw(i));
      }
      for (int i = 0; i < newPeers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 4, newPeers_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, peerId_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < oldPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(oldPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getOldPeersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newPeers_.size(); i++) {
          dataSize += computeStringSizeNoTag(newPeers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewPeersList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ResetPeerRequest)) {
        return super.equals(obj);
      }
      ResetPeerRequest other = (ResetPeerRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && getOldPeersList().equals(other.getOldPeersList());
      result = result && getNewPeersList().equals(other.getNewPeersList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      if (getOldPeersCount() > 0) {
        hash = (37 * hash) + OLD_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getOldPeersList().hashCode();
      }
      if (getNewPeersCount() > 0) {
        hash = (37 * hash) + NEW_PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewPeersList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static ResetPeerRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetPeerRequest parseFrom(java.nio.ByteBuffer data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetPeerRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetPeerRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetPeerRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetPeerRequest parseFrom(byte[] data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetPeerRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ResetPeerRequest parseFrom(java.io.InputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ResetPeerRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ResetPeerRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static ResetPeerRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ResetPeerRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ResetPeerRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.ResetPeerRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.ResetPeerRequest)
            ResetPeerRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_ResetPeerRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_ResetPeerRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(ResetPeerRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.ResetPeerRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000008);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_ResetPeerRequest_descriptor;
      }

      public ResetPeerRequest getDefaultInstanceForType() {
        return ResetPeerRequest.getDefaultInstance();
      }

      public ResetPeerRequest build() {
        ResetPeerRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ResetPeerRequest buildPartial() {
        ResetPeerRequest result = new ResetPeerRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.peerId_ = peerId_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          oldPeers_ = oldPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.oldPeers_ = oldPeers_;
        if (((bitField0_ & 0x00000008) == 0x00000008)) {
          newPeers_ = newPeers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000008);
        }
        result.newPeers_ = newPeers_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ResetPeerRequest) {
          return mergeFrom((ResetPeerRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ResetPeerRequest other) {
        if (other == ResetPeerRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000002;
          peerId_ = other.peerId_;
          onChanged();
        }
        if (!other.oldPeers_.isEmpty()) {
          if (oldPeers_.isEmpty()) {
            oldPeers_ = other.oldPeers_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureOldPeersIsMutable();
            oldPeers_.addAll(other.oldPeers_);
          }
          onChanged();
        }
        if (!other.newPeers_.isEmpty()) {
          if (newPeers_.isEmpty()) {
            newPeers_ = other.newPeers_;
            bitField0_ = (bitField0_ & ~0x00000008);
          } else {
            ensureNewPeersIsMutable();
            newPeers_.addAll(other.newPeers_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasPeerId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        ResetPeerRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ResetPeerRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>required string peer_id = 2;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string peer_id = 2;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string peer_id = 2;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string peer_id = 2;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 2;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>required string peer_id = 2;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureOldPeersIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          oldPeers_ = new com.google.protobuf.LazyStringArrayList(oldPeers_);
          bitField0_ |= 0x00000004;
        }
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList getOldPeersList() {
        return oldPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public int getOldPeersCount() {
        return oldPeers_.size();
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public String getOldPeers(int index) {
        return oldPeers_.get(index);
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public com.google.protobuf.ByteString getOldPeersBytes(int index) {
        return oldPeers_.getByteString(index);
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public Builder setOldPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public Builder addOldPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public Builder addAllOldPeers(Iterable<String> values) {
        ensureOldPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, oldPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public Builder clearOldPeers() {
        oldPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_peers = 3;</code>
       */
      public Builder addOldPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldPeersIsMutable();
        oldPeers_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewPeersIsMutable() {
        if (!((bitField0_ & 0x00000008) == 0x00000008)) {
          newPeers_ = new com.google.protobuf.LazyStringArrayList(newPeers_);
          bitField0_ |= 0x00000008;
        }
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewPeersList() {
        return newPeers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public int getNewPeersCount() {
        return newPeers_.size();
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public String getNewPeers(int index) {
        return newPeers_.get(index);
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public com.google.protobuf.ByteString getNewPeersBytes(int index) {
        return newPeers_.getByteString(index);
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public Builder setNewPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public Builder addNewPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public Builder addAllNewPeers(Iterable<String> values) {
        ensureNewPeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newPeers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public Builder clearNewPeers() {
        newPeers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000008);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_peers = 4;</code>
       */
      public Builder addNewPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewPeersIsMutable();
        newPeers_.add(value);
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.ResetPeerRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.ResetPeerRequest)
    private static final ResetPeerRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new ResetPeerRequest();
    }

    public static ResetPeerRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<ResetPeerRequest> PARSER = new com.google.protobuf.AbstractParser<ResetPeerRequest>() {
      public ResetPeerRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new ResetPeerRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<ResetPeerRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ResetPeerRequest> getParserForType() {
      return PARSER;
    }

    public ResetPeerRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface TransferLeaderRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.TransferLeaderRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>optional string peer_id = 3;</code>
     */
    boolean hasPeerId();

    /**
     * <code>optional string peer_id = 3;</code>
     */
    String getPeerId();

    /**
     * <code>optional string peer_id = 3;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();
  }

  /**
   * Protobuf type {@code jraft.TransferLeaderRequest}
   */
  public static final class TransferLeaderRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.TransferLeaderRequest)
          TransferLeaderRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use TransferLeaderRequest.newBuilder() to construct.
    private TransferLeaderRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private TransferLeaderRequest() {
      groupId_ = "";
      leaderId_ = "";
      peerId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private TransferLeaderRequest(com.google.protobuf.CodedInputStream input,
                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000004;
              peerId_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_TransferLeaderRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_TransferLeaderRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(TransferLeaderRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 3;
    private volatile Object peerId_;

    /**
     * <code>optional string peer_id = 3;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }

    /**
     * <code>optional string peer_id = 3;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>optional string peer_id = 3;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, peerId_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, peerId_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof TransferLeaderRequest)) {
        return super.equals(obj);
      }
      TransferLeaderRequest other = (TransferLeaderRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static TransferLeaderRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static TransferLeaderRequest parseFrom(java.nio.ByteBuffer data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TransferLeaderRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static TransferLeaderRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TransferLeaderRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static TransferLeaderRequest parseFrom(byte[] data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TransferLeaderRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static TransferLeaderRequest parseFrom(java.io.InputStream input,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static TransferLeaderRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static TransferLeaderRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static TransferLeaderRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static TransferLeaderRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(TransferLeaderRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.TransferLeaderRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.TransferLeaderRequest)
            TransferLeaderRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_TransferLeaderRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_TransferLeaderRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(TransferLeaderRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.TransferLeaderRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_TransferLeaderRequest_descriptor;
      }

      public TransferLeaderRequest getDefaultInstanceForType() {
        return TransferLeaderRequest.getDefaultInstance();
      }

      public TransferLeaderRequest build() {
        TransferLeaderRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public TransferLeaderRequest buildPartial() {
        TransferLeaderRequest result = new TransferLeaderRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.peerId_ = peerId_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof TransferLeaderRequest) {
          return mergeFrom((TransferLeaderRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(TransferLeaderRequest other) {
        if (other == TransferLeaderRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000004;
          peerId_ = other.peerId_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        TransferLeaderRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (TransferLeaderRequest) e
                  .getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000004);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 3;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000004;
        peerId_ = value;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.TransferLeaderRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.TransferLeaderRequest)
    private static final TransferLeaderRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new TransferLeaderRequest();
    }

    public static TransferLeaderRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<TransferLeaderRequest> PARSER = new com.google.protobuf.AbstractParser<TransferLeaderRequest>() {
      public TransferLeaderRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new TransferLeaderRequest(
                input,
                extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<TransferLeaderRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<TransferLeaderRequest> getParserForType() {
      return PARSER;
    }

    public TransferLeaderRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface GetLeaderRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.GetLeaderRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    boolean hasPeerId();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    String getPeerId();

    /**
     * <code>optional string peer_id = 2;</code>
     */
    com.google.protobuf.ByteString getPeerIdBytes();
  }

  /**
   * Protobuf type {@code jraft.GetLeaderRequest}
   */
  public static final class GetLeaderRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.GetLeaderRequest)
          GetLeaderRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use GetLeaderRequest.newBuilder() to construct.
    private GetLeaderRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private GetLeaderRequest() {
      groupId_ = "";
      peerId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private GetLeaderRequest(com.google.protobuf.CodedInputStream input,
                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              peerId_ = bs;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_GetLeaderRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_GetLeaderRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(GetLeaderRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           PEER_ID_FIELD_NUMBER = 2;
    private volatile Object peerId_;

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public boolean hasPeerId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public String getPeerId() {
      Object ref = peerId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          peerId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>optional string peer_id = 2;</code>
     */
    public com.google.protobuf.ByteString getPeerIdBytes() {
      Object ref = peerId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, peerId_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, peerId_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GetLeaderRequest)) {
        return super.equals(obj);
      }
      GetLeaderRequest other = (GetLeaderRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasPeerId() == other.hasPeerId());
      if (hasPeerId()) {
        result = result && getPeerId().equals(other.getPeerId());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasPeerId()) {
        hash = (37 * hash) + PEER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getPeerId().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static GetLeaderRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderRequest parseFrom(java.nio.ByteBuffer data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderRequest parseFrom(byte[] data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetLeaderRequest parseFrom(java.io.InputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static GetLeaderRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static GetLeaderRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static GetLeaderRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetLeaderRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(GetLeaderRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.GetLeaderRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.GetLeaderRequest)
            GetLeaderRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_GetLeaderRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_GetLeaderRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(GetLeaderRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.GetLeaderRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        peerId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_GetLeaderRequest_descriptor;
      }

      public GetLeaderRequest getDefaultInstanceForType() {
        return GetLeaderRequest.getDefaultInstance();
      }

      public GetLeaderRequest build() {
        GetLeaderRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public GetLeaderRequest buildPartial() {
        GetLeaderRequest result = new GetLeaderRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.peerId_ = peerId_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof GetLeaderRequest) {
          return mergeFrom((GetLeaderRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(GetLeaderRequest other) {
        if (other == GetLeaderRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasPeerId()) {
          bitField0_ |= 0x00000002;
          peerId_ = other.peerId_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        GetLeaderRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (GetLeaderRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object peerId_ = "";

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public boolean hasPeerId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public String getPeerId() {
        Object ref = peerId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            peerId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public com.google.protobuf.ByteString getPeerIdBytes() {
        Object ref = peerId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          peerId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder setPeerId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder clearPeerId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        peerId_ = getDefaultInstance().getPeerId();
        onChanged();
        return this;
      }

      /**
       * <code>optional string peer_id = 2;</code>
       */
      public Builder setPeerIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        peerId_ = value;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.GetLeaderRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.GetLeaderRequest)
    private static final GetLeaderRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new GetLeaderRequest();
    }

    public static GetLeaderRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<GetLeaderRequest> PARSER = new com.google.protobuf.AbstractParser<GetLeaderRequest>() {
      public GetLeaderRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new GetLeaderRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<GetLeaderRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<GetLeaderRequest> getParserForType() {
      return PARSER;
    }

    public GetLeaderRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface GetLeaderResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.GetLeaderResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string leader_id = 1;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 1;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 1;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.GetLeaderResponse}
   */
  public static final class GetLeaderResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.GetLeaderResponse)
          GetLeaderResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use GetLeaderResponse.newBuilder() to construct.
    private GetLeaderResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private GetLeaderResponse() {
      leaderId_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private GetLeaderResponse(com.google.protobuf.CodedInputStream input,
                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              leaderId_ = bs;
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000002) == 0x00000002)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000002;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_GetLeaderResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_GetLeaderResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(GetLeaderResponse.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           LEADER_ID_FIELD_NUMBER = 1;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 1;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string leader_id = 1;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 1;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, leaderId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, leaderId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GetLeaderResponse)) {
        return super.equals(obj);
      }
      GetLeaderResponse other = (GetLeaderResponse) obj;

      boolean result = true;
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static GetLeaderResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderResponse parseFrom(java.nio.ByteBuffer data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetLeaderResponse parseFrom(byte[] data,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetLeaderResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetLeaderResponse parseFrom(java.io.InputStream input,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static GetLeaderResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static GetLeaderResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static GetLeaderResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetLeaderResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(GetLeaderResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.GetLeaderResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.GetLeaderResponse)
            GetLeaderResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_GetLeaderResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_GetLeaderResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(GetLeaderResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.GetLeaderResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_GetLeaderResponse_descriptor;
      }

      public GetLeaderResponse getDefaultInstanceForType() {
        return GetLeaderResponse.getDefaultInstance();
      }

      public GetLeaderResponse build() {
        GetLeaderResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public GetLeaderResponse buildPartial() {
        GetLeaderResponse result = new GetLeaderResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.leaderId_ = leaderId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof GetLeaderResponse) {
          return mergeFrom((GetLeaderResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(GetLeaderResponse other) {
        if (other == GetLeaderResponse.getDefaultInstance())
          return this;
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000001;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasLeaderId()) {
          return false;
        }
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        GetLeaderResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (GetLeaderResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 1;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string leader_id = 1;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 1;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 1;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 1;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 1;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000002;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000002) == 0x00000002) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000002;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000002;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.GetLeaderResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.GetLeaderResponse)
    private static final GetLeaderResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new GetLeaderResponse();
    }

    public static GetLeaderResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<GetLeaderResponse> PARSER = new com.google.protobuf.AbstractParser<GetLeaderResponse>() {
      public GetLeaderResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new GetLeaderResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<GetLeaderResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<GetLeaderResponse> getParserForType() {
      return PARSER;
    }

    public GetLeaderResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface GetPeersRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.GetPeersRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>optional string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>optional string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>optional string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>optional bool only_alive = 3 [default = false];</code>
     */
    boolean hasOnlyAlive();

    /**
     * <code>optional bool only_alive = 3 [default = false];</code>
     */
    boolean getOnlyAlive();
  }

  /**
   * Protobuf type {@code jraft.GetPeersRequest}
   */
  public static final class GetPeersRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.GetPeersRequest)
          GetPeersRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use GetPeersRequest.newBuilder() to construct.
    private GetPeersRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private GetPeersRequest() {
      groupId_ = "";
      leaderId_ = "";
      onlyAlive_ = false;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private GetPeersRequest(com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 24: {
              bitField0_ |= 0x00000004;
              onlyAlive_ = input.readBool();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_GetPeersRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_GetPeersRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(GetPeersRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>optional string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>optional string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>optional string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int ONLY_ALIVE_FIELD_NUMBER = 3;
    private boolean         onlyAlive_;

    /**
     * <code>optional bool only_alive = 3 [default = false];</code>
     */
    public boolean hasOnlyAlive() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }

    /**
     * <code>optional bool only_alive = 3 [default = false];</code>
     */
    public boolean getOnlyAlive() {
      return onlyAlive_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeBool(3, onlyAlive_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream.computeBoolSize(3, onlyAlive_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GetPeersRequest)) {
        return super.equals(obj);
      }
      GetPeersRequest other = (GetPeersRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && (hasOnlyAlive() == other.hasOnlyAlive());
      if (hasOnlyAlive()) {
        result = result && (getOnlyAlive() == other.getOnlyAlive());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (hasOnlyAlive()) {
        hash = (37 * hash) + ONLY_ALIVE_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getOnlyAlive());
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static GetPeersRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersRequest parseFrom(java.nio.ByteBuffer data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersRequest parseFrom(byte[] data,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetPeersRequest parseFrom(java.io.InputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static GetPeersRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static GetPeersRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                           com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static GetPeersRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetPeersRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(GetPeersRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.GetPeersRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.GetPeersRequest)
            GetPeersRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_GetPeersRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_GetPeersRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(GetPeersRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.GetPeersRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        onlyAlive_ = false;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_GetPeersRequest_descriptor;
      }

      public GetPeersRequest getDefaultInstanceForType() {
        return GetPeersRequest.getDefaultInstance();
      }

      public GetPeersRequest build() {
        GetPeersRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public GetPeersRequest buildPartial() {
        GetPeersRequest result = new GetPeersRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.onlyAlive_ = onlyAlive_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof GetPeersRequest) {
          return mergeFrom((GetPeersRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(GetPeersRequest other) {
        if (other == GetPeersRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (other.hasOnlyAlive()) {
          setOnlyAlive(other.getOnlyAlive());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        GetPeersRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (GetPeersRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>optional string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private boolean onlyAlive_;

      /**
       * <code>optional bool only_alive = 3 [default = false];</code>
       */
      public boolean hasOnlyAlive() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional bool only_alive = 3 [default = false];</code>
       */
      public boolean getOnlyAlive() {
        return onlyAlive_;
      }

      /**
       * <code>optional bool only_alive = 3 [default = false];</code>
       */
      public Builder setOnlyAlive(boolean value) {
        bitField0_ |= 0x00000004;
        onlyAlive_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>optional bool only_alive = 3 [default = false];</code>
       */
      public Builder clearOnlyAlive() {
        bitField0_ = (bitField0_ & ~0x00000004);
        onlyAlive_ = false;
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.GetPeersRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.GetPeersRequest)
    private static final GetPeersRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new GetPeersRequest();
    }

    public static GetPeersRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<GetPeersRequest> PARSER = new com.google.protobuf.AbstractParser<GetPeersRequest>() {
      public GetPeersRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new GetPeersRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<GetPeersRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<GetPeersRequest> getParserForType() {
      return PARSER;
    }

    public GetPeersRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface GetPeersResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.GetPeersResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string peers = 1;</code>
     */
    java.util.List<String> getPeersList();

    /**
     * <code>repeated string peers = 1;</code>
     */
    int getPeersCount();

    /**
     * <code>repeated string peers = 1;</code>
     */
    String getPeers(int index);

    /**
     * <code>repeated string peers = 1;</code>
     */
    com.google.protobuf.ByteString getPeersBytes(int index);

    /**
     * <code>repeated string learners = 2;</code>
     */
    java.util.List<String> getLearnersList();

    /**
     * <code>repeated string learners = 2;</code>
     */
    int getLearnersCount();

    /**
     * <code>repeated string learners = 2;</code>
     */
    String getLearners(int index);

    /**
     * <code>repeated string learners = 2;</code>
     */
    com.google.protobuf.ByteString getLearnersBytes(int index);

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.GetPeersResponse}
   */
  public static final class GetPeersResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.GetPeersResponse)
          GetPeersResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use GetPeersResponse.newBuilder() to construct.
    private GetPeersResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private GetPeersResponse() {
      peers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private GetPeersResponse(com.google.protobuf.CodedInputStream input,
                             com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                peers_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              peers_.add(bs);
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                learners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              learners_.add(bs);
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          peers_ = peers_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          learners_ = learners_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_GetPeersResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_GetPeersResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(GetPeersResponse.class,
                      Builder.class);
    }

    private int                                bitField0_;
    public static final int                    PEERS_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList peers_;

    /**
     * <code>repeated string peers = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList getPeersList() {
      return peers_;
    }

    /**
     * <code>repeated string peers = 1;</code>
     */
    public int getPeersCount() {
      return peers_.size();
    }

    /**
     * <code>repeated string peers = 1;</code>
     */
    public String getPeers(int index) {
      return peers_.get(index);
    }

    /**
     * <code>repeated string peers = 1;</code>
     */
    public com.google.protobuf.ByteString getPeersBytes(int index) {
      return peers_.getByteString(index);
    }

    public static final int                    LEARNERS_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList learners_;

    /**
     * <code>repeated string learners = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getLearnersList() {
      return learners_;
    }

    /**
     * <code>repeated string learners = 2;</code>
     */
    public int getLearnersCount() {
      return learners_.size();
    }

    /**
     * <code>repeated string learners = 2;</code>
     */
    public String getLearners(int index) {
      return learners_.get(index);
    }

    /**
     * <code>repeated string learners = 2;</code>
     */
    public com.google.protobuf.ByteString getLearnersBytes(int index) {
      return learners_.getByteString(index);
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < peers_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, peers_.getRaw(i));
      }
      for (int i = 0; i < learners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, learners_.getRaw(i));
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < peers_.size(); i++) {
          dataSize += computeStringSizeNoTag(peers_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getPeersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < learners_.size(); i++) {
          dataSize += computeStringSizeNoTag(learners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getLearnersList().size();
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GetPeersResponse)) {
        return super.equals(obj);
      }
      GetPeersResponse other = (GetPeersResponse) obj;

      boolean result = true;
      result = result && getPeersList().equals(other.getPeersList());
      result = result && getLearnersList().equals(other.getLearnersList());
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getPeersCount() > 0) {
        hash = (37 * hash) + PEERS_FIELD_NUMBER;
        hash = (53 * hash) + getPeersList().hashCode();
      }
      if (getLearnersCount() > 0) {
        hash = (37 * hash) + LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getLearnersList().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static GetPeersResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersResponse parseFrom(java.nio.ByteBuffer data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static GetPeersResponse parseFrom(byte[] data,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static GetPeersResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetPeersResponse parseFrom(java.io.InputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static GetPeersResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static GetPeersResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static GetPeersResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static GetPeersResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(GetPeersResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.GetPeersResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.GetPeersResponse)
            GetPeersResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_GetPeersResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_GetPeersResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(GetPeersResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.GetPeersResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        peers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_GetPeersResponse_descriptor;
      }

      public GetPeersResponse getDefaultInstanceForType() {
        return GetPeersResponse.getDefaultInstance();
      }

      public GetPeersResponse build() {
        GetPeersResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public GetPeersResponse buildPartial() {
        GetPeersResponse result = new GetPeersResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          peers_ = peers_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.peers_ = peers_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          learners_ = learners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.learners_ = learners_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000001;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof GetPeersResponse) {
          return mergeFrom((GetPeersResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(GetPeersResponse other) {
        if (other == GetPeersResponse.getDefaultInstance())
          return this;
        if (!other.peers_.isEmpty()) {
          if (peers_.isEmpty()) {
            peers_ = other.peers_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensurePeersIsMutable();
            peers_.addAll(other.peers_);
          }
          onChanged();
        }
        if (!other.learners_.isEmpty()) {
          if (learners_.isEmpty()) {
            learners_ = other.learners_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureLearnersIsMutable();
            learners_.addAll(other.learners_);
          }
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        GetPeersResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (GetPeersResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int                                bitField0_;

      private com.google.protobuf.LazyStringList peers_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensurePeersIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          peers_ = new com.google.protobuf.LazyStringArrayList(peers_);
          bitField0_ |= 0x00000001;
        }
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList getPeersList() {
        return peers_.getUnmodifiableView();
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public int getPeersCount() {
        return peers_.size();
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public String getPeers(int index) {
        return peers_.get(index);
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public com.google.protobuf.ByteString getPeersBytes(int index) {
        return peers_.getByteString(index);
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public Builder setPeers(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePeersIsMutable();
        peers_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public Builder addPeers(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePeersIsMutable();
        peers_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public Builder addAllPeers(Iterable<String> values) {
        ensurePeersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, peers_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public Builder clearPeers() {
        peers_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string peers = 1;</code>
       */
      public Builder addPeersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePeersIsMutable();
        peers_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureLearnersIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          learners_ = new com.google.protobuf.LazyStringArrayList(learners_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public com.google.protobuf.ProtocolStringList getLearnersList() {
        return learners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public int getLearnersCount() {
        return learners_.size();
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public String getLearners(int index) {
        return learners_.get(index);
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public com.google.protobuf.ByteString getLearnersBytes(int index) {
        return learners_.getByteString(index);
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public Builder setLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public Builder addLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public Builder addAllLearners(Iterable<String> values) {
        ensureLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, learners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public Builder clearLearners() {
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 2;</code>
       */
      public Builder addLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.GetPeersResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.GetPeersResponse)
    private static final GetPeersResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new GetPeersResponse();
    }

    public static GetPeersResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<GetPeersResponse> PARSER = new com.google.protobuf.AbstractParser<GetPeersResponse>() {
      public GetPeersResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new GetPeersResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<GetPeersResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<GetPeersResponse> getParserForType() {
      return PARSER;
    }

    public GetPeersResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface AddLearnersRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.AddLearnersRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>repeated string learners = 3;</code>
     */
    java.util.List<String> getLearnersList();

    /**
     * <code>repeated string learners = 3;</code>
     */
    int getLearnersCount();

    /**
     * <code>repeated string learners = 3;</code>
     */
    String getLearners(int index);

    /**
     * <code>repeated string learners = 3;</code>
     */
    com.google.protobuf.ByteString getLearnersBytes(int index);
  }

  /**
   * Protobuf type {@code jraft.AddLearnersRequest}
   */
  public static final class AddLearnersRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.AddLearnersRequest)
          AddLearnersRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use AddLearnersRequest.newBuilder() to construct.
    private AddLearnersRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private AddLearnersRequest() {
      groupId_ = "";
      leaderId_ = "";
      learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private AddLearnersRequest(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                learners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              learners_.add(bs);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_AddLearnersRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_AddLearnersRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(AddLearnersRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                    LEARNERS_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList learners_;

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList getLearnersList() {
      return learners_;
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public int getLearnersCount() {
      return learners_.size();
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public String getLearners(int index) {
      return learners_.get(index);
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ByteString getLearnersBytes(int index) {
      return learners_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      for (int i = 0; i < learners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, learners_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < learners_.size(); i++) {
          dataSize += computeStringSizeNoTag(learners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getLearnersList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof AddLearnersRequest)) {
        return super.equals(obj);
      }
      AddLearnersRequest other = (AddLearnersRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && getLearnersList().equals(other.getLearnersList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (getLearnersCount() > 0) {
        hash = (37 * hash) + LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getLearnersList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static AddLearnersRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddLearnersRequest parseFrom(java.nio.ByteBuffer data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddLearnersRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddLearnersRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddLearnersRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static AddLearnersRequest parseFrom(byte[] data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static AddLearnersRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddLearnersRequest parseFrom(java.io.InputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static AddLearnersRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static AddLearnersRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static AddLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static AddLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(AddLearnersRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.AddLearnersRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.AddLearnersRequest)
            AddLearnersRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_AddLearnersRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_AddLearnersRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(AddLearnersRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.AddLearnersRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_AddLearnersRequest_descriptor;
      }

      public AddLearnersRequest getDefaultInstanceForType() {
        return AddLearnersRequest.getDefaultInstance();
      }

      public AddLearnersRequest build() {
        AddLearnersRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public AddLearnersRequest buildPartial() {
        AddLearnersRequest result = new AddLearnersRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.learners_ = learners_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof AddLearnersRequest) {
          return mergeFrom((AddLearnersRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(AddLearnersRequest other) {
        if (other == AddLearnersRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (!other.learners_.isEmpty()) {
          if (learners_.isEmpty()) {
            learners_ = other.learners_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureLearnersIsMutable();
            learners_.addAll(other.learners_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        AddLearnersRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (AddLearnersRequest) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureLearnersIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = new com.google.protobuf.LazyStringArrayList(learners_);
          bitField0_ |= 0x00000004;
        }
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList getLearnersList() {
        return learners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public int getLearnersCount() {
        return learners_.size();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public String getLearners(int index) {
        return learners_.get(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ByteString getLearnersBytes(int index) {
        return learners_.getByteString(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder setLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addAllLearners(Iterable<String> values) {
        ensureLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, learners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder clearLearners() {
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.AddLearnersRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.AddLearnersRequest)
    private static final AddLearnersRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new AddLearnersRequest();
    }

    public static AddLearnersRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<AddLearnersRequest> PARSER = new com.google.protobuf.AbstractParser<AddLearnersRequest>() {
      public AddLearnersRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new AddLearnersRequest(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<AddLearnersRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<AddLearnersRequest> getParserForType() {
      return PARSER;
    }

    public AddLearnersRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface RemoveLearnersRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.RemoveLearnersRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>repeated string learners = 3;</code>
     */
    java.util.List<String> getLearnersList();

    /**
     * <code>repeated string learners = 3;</code>
     */
    int getLearnersCount();

    /**
     * <code>repeated string learners = 3;</code>
     */
    String getLearners(int index);

    /**
     * <code>repeated string learners = 3;</code>
     */
    com.google.protobuf.ByteString getLearnersBytes(int index);
  }

  /**
   * Protobuf type {@code jraft.RemoveLearnersRequest}
   */
  public static final class RemoveLearnersRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.RemoveLearnersRequest)
          RemoveLearnersRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use RemoveLearnersRequest.newBuilder() to construct.
    private RemoveLearnersRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private RemoveLearnersRequest() {
      groupId_ = "";
      leaderId_ = "";
      learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private RemoveLearnersRequest(com.google.protobuf.CodedInputStream input,
                                  com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                learners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              learners_.add(bs);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_RemoveLearnersRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_RemoveLearnersRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(RemoveLearnersRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                    LEARNERS_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList learners_;

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList getLearnersList() {
      return learners_;
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public int getLearnersCount() {
      return learners_.size();
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public String getLearners(int index) {
      return learners_.get(index);
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ByteString getLearnersBytes(int index) {
      return learners_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      for (int i = 0; i < learners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, learners_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < learners_.size(); i++) {
          dataSize += computeStringSizeNoTag(learners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getLearnersList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof RemoveLearnersRequest)) {
        return super.equals(obj);
      }
      RemoveLearnersRequest other = (RemoveLearnersRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && getLearnersList().equals(other.getLearnersList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (getLearnersCount() > 0) {
        hash = (37 * hash) + LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getLearnersList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static RemoveLearnersRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemoveLearnersRequest parseFrom(java.nio.ByteBuffer data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemoveLearnersRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemoveLearnersRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemoveLearnersRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static RemoveLearnersRequest parseFrom(byte[] data,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static RemoveLearnersRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemoveLearnersRequest parseFrom(java.io.InputStream input,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static RemoveLearnersRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static RemoveLearnersRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static RemoveLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static RemoveLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(RemoveLearnersRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.RemoveLearnersRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.RemoveLearnersRequest)
            RemoveLearnersRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_RemoveLearnersRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_RemoveLearnersRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(RemoveLearnersRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.RemoveLearnersRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_RemoveLearnersRequest_descriptor;
      }

      public RemoveLearnersRequest getDefaultInstanceForType() {
        return RemoveLearnersRequest.getDefaultInstance();
      }

      public RemoveLearnersRequest build() {
        RemoveLearnersRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public RemoveLearnersRequest buildPartial() {
        RemoveLearnersRequest result = new RemoveLearnersRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.learners_ = learners_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof RemoveLearnersRequest) {
          return mergeFrom((RemoveLearnersRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(RemoveLearnersRequest other) {
        if (other == RemoveLearnersRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (!other.learners_.isEmpty()) {
          if (learners_.isEmpty()) {
            learners_ = other.learners_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureLearnersIsMutable();
            learners_.addAll(other.learners_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        RemoveLearnersRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (RemoveLearnersRequest) e
                  .getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureLearnersIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = new com.google.protobuf.LazyStringArrayList(learners_);
          bitField0_ |= 0x00000004;
        }
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList getLearnersList() {
        return learners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public int getLearnersCount() {
        return learners_.size();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public String getLearners(int index) {
        return learners_.get(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ByteString getLearnersBytes(int index) {
        return learners_.getByteString(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder setLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addAllLearners(Iterable<String> values) {
        ensureLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, learners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder clearLearners() {
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.RemoveLearnersRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.RemoveLearnersRequest)
    private static final RemoveLearnersRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new RemoveLearnersRequest();
    }

    public static RemoveLearnersRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<RemoveLearnersRequest> PARSER = new com.google.protobuf.AbstractParser<RemoveLearnersRequest>() {
      public RemoveLearnersRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new RemoveLearnersRequest(
                input,
                extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<RemoveLearnersRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<RemoveLearnersRequest> getParserForType() {
      return PARSER;
    }

    public RemoveLearnersRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface ResetLearnersRequestOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.ResetLearnersRequest)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string group_id = 1;</code>
     */
    boolean hasGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    String getGroupId();

    /**
     * <code>required string group_id = 1;</code>
     */
    com.google.protobuf.ByteString getGroupIdBytes();

    /**
     * <code>required string leader_id = 2;</code>
     */
    boolean hasLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    String getLeaderId();

    /**
     * <code>required string leader_id = 2;</code>
     */
    com.google.protobuf.ByteString getLeaderIdBytes();

    /**
     * <code>repeated string learners = 3;</code>
     */
    java.util.List<String> getLearnersList();

    /**
     * <code>repeated string learners = 3;</code>
     */
    int getLearnersCount();

    /**
     * <code>repeated string learners = 3;</code>
     */
    String getLearners(int index);

    /**
     * <code>repeated string learners = 3;</code>
     */
    com.google.protobuf.ByteString getLearnersBytes(int index);
  }

  /**
   * Protobuf type {@code jraft.ResetLearnersRequest}
   */
  public static final class ResetLearnersRequest extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.ResetLearnersRequest)
          ResetLearnersRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ResetLearnersRequest.newBuilder() to construct.
    private ResetLearnersRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private ResetLearnersRequest() {
      groupId_ = "";
      leaderId_ = "";
      learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private ResetLearnersRequest(com.google.protobuf.CodedInputStream input,
                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              groupId_ = bs;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000002;
              leaderId_ = bs;
              break;
            }
            case 26: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                learners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000004;
              }
              learners_.add(bs);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_ResetLearnersRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_ResetLearnersRequest_fieldAccessorTable
              .ensureFieldAccessorsInitialized(ResetLearnersRequest.class,
                      Builder.class);
    }

    private int                       bitField0_;
    public static final int           GROUP_ID_FIELD_NUMBER = 1;
    private volatile Object groupId_;

    /**
     * <code>required string group_id = 1;</code>
     */
    public boolean hasGroupId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public String getGroupId() {
      Object ref = groupId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          groupId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string group_id = 1;</code>
     */
    public com.google.protobuf.ByteString getGroupIdBytes() {
      Object ref = groupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        groupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int           LEADER_ID_FIELD_NUMBER = 2;
    private volatile Object leaderId_;

    /**
     * <code>required string leader_id = 2;</code>
     */
    public boolean hasLeaderId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public String getLeaderId() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          leaderId_ = s;
        }
        return s;
      }
    }

    /**
     * <code>required string leader_id = 2;</code>
     */
    public com.google.protobuf.ByteString getLeaderIdBytes() {
      Object ref = leaderId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        leaderId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int                    LEARNERS_FIELD_NUMBER = 3;
    private com.google.protobuf.LazyStringList learners_;

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ProtocolStringList getLearnersList() {
      return learners_;
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public int getLearnersCount() {
      return learners_.size();
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public String getLearners(int index) {
      return learners_.get(index);
    }

    /**
     * <code>repeated string learners = 3;</code>
     */
    public com.google.protobuf.ByteString getLearnersBytes(int index) {
      return learners_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (!hasGroupId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLeaderId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, leaderId_);
      }
      for (int i = 0; i < learners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, learners_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, groupId_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, leaderId_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < learners_.size(); i++) {
          dataSize += computeStringSizeNoTag(learners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getLearnersList().size();
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ResetLearnersRequest)) {
        return super.equals(obj);
      }
      ResetLearnersRequest other = (ResetLearnersRequest) obj;

      boolean result = true;
      result = result && (hasGroupId() == other.hasGroupId());
      if (hasGroupId()) {
        result = result && getGroupId().equals(other.getGroupId());
      }
      result = result && (hasLeaderId() == other.hasLeaderId());
      if (hasLeaderId()) {
        result = result && getLeaderId().equals(other.getLeaderId());
      }
      result = result && getLearnersList().equals(other.getLearnersList());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasGroupId()) {
        hash = (37 * hash) + GROUP_ID_FIELD_NUMBER;
        hash = (53 * hash) + getGroupId().hashCode();
      }
      if (hasLeaderId()) {
        hash = (37 * hash) + LEADER_ID_FIELD_NUMBER;
        hash = (53 * hash) + getLeaderId().hashCode();
      }
      if (getLearnersCount() > 0) {
        hash = (37 * hash) + LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getLearnersList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static ResetLearnersRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetLearnersRequest parseFrom(java.nio.ByteBuffer data,
                                                                                       com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetLearnersRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetLearnersRequest parseFrom(com.google.protobuf.ByteString data,
                                                                                       com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetLearnersRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static ResetLearnersRequest parseFrom(byte[] data,
                                                                                       com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ResetLearnersRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ResetLearnersRequest parseFrom(java.io.InputStream input,
                                                                                       com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ResetLearnersRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ResetLearnersRequest parseDelimitedFrom(java.io.InputStream input,
                                                                                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static ResetLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ResetLearnersRequest parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                       com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ResetLearnersRequest prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.ResetLearnersRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.ResetLearnersRequest)
            ResetLearnersRequestOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_ResetLearnersRequest_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_ResetLearnersRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(ResetLearnersRequest.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.ResetLearnersRequest.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        }
      }

      public Builder clear() {
        super.clear();
        groupId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        leaderId_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_ResetLearnersRequest_descriptor;
      }

      public ResetLearnersRequest getDefaultInstanceForType() {
        return ResetLearnersRequest.getDefaultInstance();
      }

      public ResetLearnersRequest build() {
        ResetLearnersRequest result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public ResetLearnersRequest buildPartial() {
        ResetLearnersRequest result = new ResetLearnersRequest(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.groupId_ = groupId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.leaderId_ = leaderId_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = learners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.learners_ = learners_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof ResetLearnersRequest) {
          return mergeFrom((ResetLearnersRequest) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(ResetLearnersRequest other) {
        if (other == ResetLearnersRequest.getDefaultInstance())
          return this;
        if (other.hasGroupId()) {
          bitField0_ |= 0x00000001;
          groupId_ = other.groupId_;
          onChanged();
        }
        if (other.hasLeaderId()) {
          bitField0_ |= 0x00000002;
          leaderId_ = other.leaderId_;
          onChanged();
        }
        if (!other.learners_.isEmpty()) {
          if (learners_.isEmpty()) {
            learners_ = other.learners_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureLearnersIsMutable();
            learners_.addAll(other.learners_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (!hasGroupId()) {
          return false;
        }
        if (!hasLeaderId()) {
          return false;
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        ResetLearnersRequest parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (ResetLearnersRequest) e
                  .getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int              bitField0_;

      private Object groupId_ = "";

      /**
       * <code>required string group_id = 1;</code>
       */
      public boolean hasGroupId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public String getGroupId() {
        Object ref = groupId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            groupId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public com.google.protobuf.ByteString getGroupIdBytes() {
        Object ref = groupId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          groupId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder clearGroupId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        groupId_ = getDefaultInstance().getGroupId();
        onChanged();
        return this;
      }

      /**
       * <code>required string group_id = 1;</code>
       */
      public Builder setGroupIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000001;
        groupId_ = value;
        onChanged();
        return this;
      }

      private Object leaderId_ = "";

      /**
       * <code>required string leader_id = 2;</code>
       */
      public boolean hasLeaderId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public String getLeaderId() {
        Object ref = leaderId_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            leaderId_ = s;
          }
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public com.google.protobuf.ByteString getLeaderIdBytes() {
        Object ref = leaderId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = com.google.protobuf.ByteString
                  .copyFromUtf8((String) ref);
          leaderId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderId(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder clearLeaderId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        leaderId_ = getDefaultInstance().getLeaderId();
        onChanged();
        return this;
      }

      /**
       * <code>required string leader_id = 2;</code>
       */
      public Builder setLeaderIdBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000002;
        leaderId_ = value;
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureLearnersIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          learners_ = new com.google.protobuf.LazyStringArrayList(learners_);
          bitField0_ |= 0x00000004;
        }
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ProtocolStringList getLearnersList() {
        return learners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public int getLearnersCount() {
        return learners_.size();
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public String getLearners(int index) {
        return learners_.get(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public com.google.protobuf.ByteString getLearnersBytes(int index) {
        return learners_.getByteString(index);
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder setLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addAllLearners(Iterable<String> values) {
        ensureLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, learners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder clearLearners() {
        learners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string learners = 3;</code>
       */
      public Builder addLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureLearnersIsMutable();
        learners_.add(value);
        onChanged();
        return this;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.ResetLearnersRequest)
    }

    // @@protoc_insertion_point(class_scope:jraft.ResetLearnersRequest)
    private static final ResetLearnersRequest DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new ResetLearnersRequest();
    }

    public static ResetLearnersRequest getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<ResetLearnersRequest> PARSER = new com.google.protobuf.AbstractParser<ResetLearnersRequest>() {
      public ResetLearnersRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                   com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new ResetLearnersRequest(
                input,
                extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<ResetLearnersRequest> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ResetLearnersRequest> getParserForType() {
      return PARSER;
    }

    public ResetLearnersRequest getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface LearnersOpResponseOrBuilder extends
          // @@protoc_insertion_point(interface_extends:jraft.LearnersOpResponse)
          com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    java.util.List<String> getOldLearnersList();

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    int getOldLearnersCount();

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    String getOldLearners(int index);

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    com.google.protobuf.ByteString getOldLearnersBytes(int index);

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    java.util.List<String> getNewLearnersList();

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    int getNewLearnersCount();

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    String getNewLearners(int index);

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    com.google.protobuf.ByteString getNewLearnersBytes(int index);

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    boolean hasErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponse getErrorResponse();

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder();
  }

  /**
   * Protobuf type {@code jraft.LearnersOpResponse}
   */
  public static final class LearnersOpResponse extends com.google.protobuf.GeneratedMessageV3 implements
          // @@protoc_insertion_point(message_implements:jraft.LearnersOpResponse)
          LearnersOpResponseOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use LearnersOpResponse.newBuilder() to construct.
    private LearnersOpResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private LearnersOpResponse() {
      oldLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      newLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
      return this.unknownFields;
    }

    private LearnersOpResponse(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
              .newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                oldLearners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              oldLearners_.add(bs);
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                newLearners_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              newLearners_.add(bs);
              break;
            }
            case 794: {
              RpcRequests.ErrorResponse.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) == 0x00000001)) {
                subBuilder = errorResponse_.toBuilder();
              }
              errorResponse_ = input.readMessage(
                      RpcRequests.ErrorResponse.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(errorResponse_);
                errorResponse_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          oldLearners_ = oldLearners_.getUnmodifiableView();
        }
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          newLearners_ = newLearners_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return CliRequests.internal_static_jraft_LearnersOpResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return CliRequests.internal_static_jraft_LearnersOpResponse_fieldAccessorTable
              .ensureFieldAccessorsInitialized(LearnersOpResponse.class,
                      Builder.class);
    }

    private int                                bitField0_;
    public static final int                    OLD_LEARNERS_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList oldLearners_;

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList getOldLearnersList() {
      return oldLearners_;
    }

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    public int getOldLearnersCount() {
      return oldLearners_.size();
    }

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    public String getOldLearners(int index) {
      return oldLearners_.get(index);
    }

    /**
     * <code>repeated string old_learners = 1;</code>
     */
    public com.google.protobuf.ByteString getOldLearnersBytes(int index) {
      return oldLearners_.getByteString(index);
    }

    public static final int                    NEW_LEARNERS_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList newLearners_;

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getNewLearnersList() {
      return newLearners_;
    }

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    public int getNewLearnersCount() {
      return newLearners_.size();
    }

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    public String getNewLearners(int index) {
      return newLearners_.get(index);
    }

    /**
     * <code>repeated string new_learners = 2;</code>
     */
    public com.google.protobuf.ByteString getNewLearnersBytes(int index) {
      return newLearners_.getByteString(index);
    }

    public static final int                                     ERRORRESPONSE_FIELD_NUMBER = 99;
    private RpcRequests.ErrorResponse errorResponse_;

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public boolean hasErrorResponse() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponse getErrorResponse() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    /**
     * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
     */
    public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
      return errorResponse_ == null ? RpcRequests.ErrorResponse.getDefaultInstance()
              : errorResponse_;
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1)
        return true;
      if (isInitialized == 0)
        return false;

      if (hasErrorResponse()) {
        if (!getErrorResponse().isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
      for (int i = 0; i < oldLearners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, oldLearners_.getRaw(i));
      }
      for (int i = 0; i < newLearners_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, newLearners_.getRaw(i));
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeMessage(99, getErrorResponse());
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1)
        return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < oldLearners_.size(); i++) {
          dataSize += computeStringSizeNoTag(oldLearners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getOldLearnersList().size();
      }
      {
        int dataSize = 0;
        for (int i = 0; i < newLearners_.size(); i++) {
          dataSize += computeStringSizeNoTag(newLearners_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getNewLearnersList().size();
      }
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream.computeMessageSize(99, getErrorResponse());
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof LearnersOpResponse)) {
        return super.equals(obj);
      }
      LearnersOpResponse other = (LearnersOpResponse) obj;

      boolean result = true;
      result = result && getOldLearnersList().equals(other.getOldLearnersList());
      result = result && getNewLearnersList().equals(other.getNewLearnersList());
      result = result && (hasErrorResponse() == other.hasErrorResponse());
      if (hasErrorResponse()) {
        result = result && getErrorResponse().equals(other.getErrorResponse());
      }
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getOldLearnersCount() > 0) {
        hash = (37 * hash) + OLD_LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getOldLearnersList().hashCode();
      }
      if (getNewLearnersCount() > 0) {
        hash = (37 * hash) + NEW_LEARNERS_FIELD_NUMBER;
        hash = (53 * hash) + getNewLearnersList().hashCode();
      }
      if (hasErrorResponse()) {
        hash = (37 * hash) + ERRORRESPONSE_FIELD_NUMBER;
        hash = (53 * hash) + getErrorResponse().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static LearnersOpResponse parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static LearnersOpResponse parseFrom(java.nio.ByteBuffer data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static LearnersOpResponse parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static LearnersOpResponse parseFrom(com.google.protobuf.ByteString data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static LearnersOpResponse parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static LearnersOpResponse parseFrom(byte[] data,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static LearnersOpResponse parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static LearnersOpResponse parseFrom(java.io.InputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static LearnersOpResponse parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static LearnersOpResponse parseDelimitedFrom(java.io.InputStream input,
                                                                                              com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
              extensionRegistry);
    }

    public static LearnersOpResponse parseFrom(com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static LearnersOpResponse parseFrom(com.google.protobuf.CodedInputStream input,
                                                                                     com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(LearnersOpResponse prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code jraft.LearnersOpResponse}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:jraft.LearnersOpResponse)
            LearnersOpResponseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return CliRequests.internal_static_jraft_LearnersOpResponse_descriptor;
      }

      protected FieldAccessorTable internalGetFieldAccessorTable() {
        return CliRequests.internal_static_jraft_LearnersOpResponse_fieldAccessorTable
                .ensureFieldAccessorsInitialized(LearnersOpResponse.class,
                        Builder.class);
      }

      // Construct using com.alipay.sofa.jraft.rpc.CliRequests.LearnersOpResponse.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
          getErrorResponseFieldBuilder();
        }
      }

      public Builder clear() {
        super.clear();
        oldLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        newLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
        return CliRequests.internal_static_jraft_LearnersOpResponse_descriptor;
      }

      public LearnersOpResponse getDefaultInstanceForType() {
        return LearnersOpResponse.getDefaultInstance();
      }

      public LearnersOpResponse build() {
        LearnersOpResponse result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public LearnersOpResponse buildPartial() {
        LearnersOpResponse result = new LearnersOpResponse(
                this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          oldLearners_ = oldLearners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.oldLearners_ = oldLearners_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          newLearners_ = newLearners_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.newLearners_ = newLearners_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000001;
        }
        if (errorResponseBuilder_ == null) {
          result.errorResponse_ = errorResponse_;
        } else {
          result.errorResponse_ = errorResponseBuilder_.build();
        }
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }

      public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
        return (Builder) super.setField(field, value);
      }

      public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                                      Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                                      Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof LearnersOpResponse) {
          return mergeFrom((LearnersOpResponse) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(LearnersOpResponse other) {
        if (other == LearnersOpResponse.getDefaultInstance())
          return this;
        if (!other.oldLearners_.isEmpty()) {
          if (oldLearners_.isEmpty()) {
            oldLearners_ = other.oldLearners_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureOldLearnersIsMutable();
            oldLearners_.addAll(other.oldLearners_);
          }
          onChanged();
        }
        if (!other.newLearners_.isEmpty()) {
          if (newLearners_.isEmpty()) {
            newLearners_ = other.newLearners_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureNewLearnersIsMutable();
            newLearners_.addAll(other.newLearners_);
          }
          onChanged();
        }
        if (other.hasErrorResponse()) {
          mergeErrorResponse(other.getErrorResponse());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        if (hasErrorResponse()) {
          if (!getErrorResponse().isInitialized()) {
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                               com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws java.io.IOException {
        LearnersOpResponse parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (LearnersOpResponse) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int                                bitField0_;

      private com.google.protobuf.LazyStringList oldLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureOldLearnersIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          oldLearners_ = new com.google.protobuf.LazyStringArrayList(oldLearners_);
          bitField0_ |= 0x00000001;
        }
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList getOldLearnersList() {
        return oldLearners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public int getOldLearnersCount() {
        return oldLearners_.size();
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public String getOldLearners(int index) {
        return oldLearners_.get(index);
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public com.google.protobuf.ByteString getOldLearnersBytes(int index) {
        return oldLearners_.getByteString(index);
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public Builder setOldLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldLearnersIsMutable();
        oldLearners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public Builder addOldLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldLearnersIsMutable();
        oldLearners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public Builder addAllOldLearners(Iterable<String> values) {
        ensureOldLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, oldLearners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public Builder clearOldLearners() {
        oldLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string old_learners = 1;</code>
       */
      public Builder addOldLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureOldLearnersIsMutable();
        oldLearners_.add(value);
        onChanged();
        return this;
      }

      private com.google.protobuf.LazyStringList newLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;

      private void ensureNewLearnersIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          newLearners_ = new com.google.protobuf.LazyStringArrayList(newLearners_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public com.google.protobuf.ProtocolStringList getNewLearnersList() {
        return newLearners_.getUnmodifiableView();
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public int getNewLearnersCount() {
        return newLearners_.size();
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public String getNewLearners(int index) {
        return newLearners_.get(index);
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public com.google.protobuf.ByteString getNewLearnersBytes(int index) {
        return newLearners_.getByteString(index);
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public Builder setNewLearners(int index, String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewLearnersIsMutable();
        newLearners_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public Builder addNewLearners(String value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewLearnersIsMutable();
        newLearners_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public Builder addAllNewLearners(Iterable<String> values) {
        ensureNewLearnersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, newLearners_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public Builder clearNewLearners() {
        newLearners_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>repeated string new_learners = 2;</code>
       */
      public Builder addNewLearnersBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNewLearnersIsMutable();
        newLearners_.add(value);
        onChanged();
        return this;
      }

      private RpcRequests.ErrorResponse                                                                                                                                                                      errorResponse_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> errorResponseBuilder_;

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public boolean hasErrorResponse() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse getErrorResponse() {
        if (errorResponseBuilder_ == null) {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        } else {
          return errorResponseBuilder_.getMessage();
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          errorResponse_ = value;
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder setErrorResponse(RpcRequests.ErrorResponse.Builder builderForValue) {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = builderForValue.build();
          onChanged();
        } else {
          errorResponseBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder mergeErrorResponse(RpcRequests.ErrorResponse value) {
        if (errorResponseBuilder_ == null) {
          if (((bitField0_ & 0x00000004) == 0x00000004) && errorResponse_ != null
                  && errorResponse_ != RpcRequests.ErrorResponse.getDefaultInstance()) {
            errorResponse_ = RpcRequests.ErrorResponse.newBuilder(errorResponse_)
                    .mergeFrom(value).buildPartial();
          } else {
            errorResponse_ = value;
          }
          onChanged();
        } else {
          errorResponseBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000004;
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public Builder clearErrorResponse() {
        if (errorResponseBuilder_ == null) {
          errorResponse_ = null;
          onChanged();
        } else {
          errorResponseBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponse.Builder getErrorResponseBuilder() {
        bitField0_ |= 0x00000004;
        onChanged();
        return getErrorResponseFieldBuilder().getBuilder();
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      public RpcRequests.ErrorResponseOrBuilder getErrorResponseOrBuilder() {
        if (errorResponseBuilder_ != null) {
          return errorResponseBuilder_.getMessageOrBuilder();
        } else {
          return errorResponse_ == null ? RpcRequests.ErrorResponse
                  .getDefaultInstance() : errorResponse_;
        }
      }

      /**
       * <code>optional .jraft.ErrorResponse errorResponse = 99;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder> getErrorResponseFieldBuilder() {
        if (errorResponseBuilder_ == null) {
          errorResponseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<RpcRequests.ErrorResponse, RpcRequests.ErrorResponse.Builder, RpcRequests.ErrorResponseOrBuilder>(
                  getErrorResponse(), getParentForChildren(), isClean());
          errorResponse_ = null;
        }
        return errorResponseBuilder_;
      }

      public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }

      // @@protoc_insertion_point(builder_scope:jraft.LearnersOpResponse)
    }

    // @@protoc_insertion_point(class_scope:jraft.LearnersOpResponse)
    private static final LearnersOpResponse DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new LearnersOpResponse();
    }

    public static LearnersOpResponse getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @Deprecated
    public static final com.google.protobuf.Parser<LearnersOpResponse> PARSER = new com.google.protobuf.AbstractParser<LearnersOpResponse>() {
      public LearnersOpResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
                                                 com.google.protobuf.ExtensionRegistryLite extensionRegistry)
              throws com.google.protobuf.InvalidProtocolBufferException {
        return new LearnersOpResponse(
                input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<LearnersOpResponse> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<LearnersOpResponse> getParserForType() {
      return PARSER;
    }

    public LearnersOpResponse getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_AddPeerRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_AddPeerRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_AddPeerResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_AddPeerResponse_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_RemovePeerRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_RemovePeerRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_RemovePeerResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_RemovePeerResponse_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_ChangePeersRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_ChangePeersRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_ChangePeersResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_ChangePeersResponse_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_SnapshotRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_SnapshotRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_ResetPeerRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_ResetPeerRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_TransferLeaderRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_TransferLeaderRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_GetLeaderRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_GetLeaderRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_GetLeaderResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_GetLeaderResponse_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_GetPeersRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_GetPeersRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_GetPeersResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_GetPeersResponse_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_AddLearnersRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_AddLearnersRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_RemoveLearnersRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_RemoveLearnersRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_ResetLearnersRequest_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_ResetLearnersRequest_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor                internal_static_jraft_LearnersOpResponse_descriptor;
  private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_jraft_LearnersOpResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    String[] descriptorData = { "\n\tcli.proto\022\005jraft\032\trpc.proto\"F\n\016AddPeer"
            + "Request\022\020\n\010group_id\030\001 \002(\t\022\021\n\tleader_id\030\002"
            + " \002(\t\022\017\n\007peer_id\030\003 \002(\t\"d\n\017AddPeerResponse"
            + "\022\021\n\told_peers\030\001 \003(\t\022\021\n\tnew_peers\030\002 \003(\t\022+"
            + "\n\rerrorResponse\030c \001(\0132\024.jraft.ErrorRespo"
            + "nse\"I\n\021RemovePeerRequest\022\020\n\010group_id\030\001 \002"
            + "(\t\022\021\n\tleader_id\030\002 \002(\t\022\017\n\007peer_id\030\003 \002(\t\"g"
            + "\n\022RemovePeerResponse\022\021\n\told_peers\030\001 \003(\t\022"
            + "\021\n\tnew_peers\030\002 \003(\t\022+\n\rerrorResponse\030c \001("
            + "\0132\024.jraft.ErrorResponse\"L\n\022ChangePeersRe"
            + "quest\022\020\n\010group_id\030\001 \002(\t\022\021\n\tleader_id\030\002 \002"
            + "(\t\022\021\n\tnew_peers\030\003 \003(\t\"h\n\023ChangePeersResp"
            + "onse\022\021\n\told_peers\030\001 \003(\t\022\021\n\tnew_peers\030\002 \003"
            + "(\t\022+\n\rerrorResponse\030c \001(\0132\024.jraft.ErrorR"
            + "esponse\"4\n\017SnapshotRequest\022\020\n\010group_id\030\001"
            + " \002(\t\022\017\n\007peer_id\030\002 \001(\t\"[\n\020ResetPeerReques"
            + "t\022\020\n\010group_id\030\001 \002(\t\022\017\n\007peer_id\030\002 \002(\t\022\021\n\t"
            + "old_peers\030\003 \003(\t\022\021\n\tnew_peers\030\004 \003(\t\"M\n\025Tr"
            + "ansferLeaderRequest\022\020\n\010group_id\030\001 \002(\t\022\021\n"
            + "\tleader_id\030\002 \002(\t\022\017\n\007peer_id\030\003 \001(\t\"5\n\020Get"
            + "LeaderRequest\022\020\n\010group_id\030\001 \002(\t\022\017\n\007peer_"
            + "id\030\002 \001(\t\"S\n\021GetLeaderResponse\022\021\n\tleader_"
            + "id\030\001 \002(\t\022+\n\rerrorResponse\030c \001(\0132\024.jraft."
            + "ErrorResponse\"Q\n\017GetPeersRequest\022\020\n\010grou"
            + "p_id\030\001 \002(\t\022\021\n\tleader_id\030\002 \001(\t\022\031\n\nonly_al"
            + "ive\030\003 \001(\010:\005false\"`\n\020GetPeersResponse\022\r\n\005"
            + "peers\030\001 \003(\t\022\020\n\010learners\030\002 \003(\t\022+\n\rerrorRe"
            + "sponse\030c \001(\0132\024.jraft.ErrorResponse\"K\n\022Ad"
            + "dLearnersRequest\022\020\n\010group_id\030\001 \002(\t\022\021\n\tle"
            + "ader_id\030\002 \002(\t\022\020\n\010learners\030\003 \003(\t\"N\n\025Remov"
            + "eLearnersRequest\022\020\n\010group_id\030\001 \002(\t\022\021\n\tle"
            + "ader_id\030\002 \002(\t\022\020\n\010learners\030\003 \003(\t\"M\n\024Reset"
            + "LearnersRequest\022\020\n\010group_id\030\001 \002(\t\022\021\n\tlea"
            + "der_id\030\002 \002(\t\022\020\n\010learners\030\003 \003(\t\"m\n\022Learne"
            + "rsOpResponse\022\024\n\014old_learners\030\001 \003(\t\022\024\n\014ne"
            + "w_learners\030\002 \003(\t\022+\n\rerrorResponse\030c \001(\0132"
            + "\024.jraft.ErrorResponseB(\n\031com.alipay.sofa"
            + ".jraft.rpcB\013CliRequests" };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
      public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
        descriptor = root;
        return null;
      }
    };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] { RpcRequests
                    .getDescriptor(), }, assigner);
    internal_static_jraft_AddPeerRequest_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_jraft_AddPeerRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_AddPeerRequest_descriptor,
            new String[] { "GroupId", "LeaderId", "PeerId", });
    internal_static_jraft_AddPeerResponse_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_jraft_AddPeerResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_AddPeerResponse_descriptor, new String[] { "OldPeers", "NewPeers",
            "ErrorResponse", });
    internal_static_jraft_RemovePeerRequest_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_jraft_RemovePeerRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_RemovePeerRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "PeerId", });
    internal_static_jraft_RemovePeerResponse_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_jraft_RemovePeerResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_RemovePeerResponse_descriptor, new String[] { "OldPeers", "NewPeers",
            "ErrorResponse", });
    internal_static_jraft_ChangePeersRequest_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_jraft_ChangePeersRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_ChangePeersRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "NewPeers", });
    internal_static_jraft_ChangePeersResponse_descriptor = getDescriptor().getMessageTypes().get(5);
    internal_static_jraft_ChangePeersResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_ChangePeersResponse_descriptor, new String[] { "OldPeers", "NewPeers",
            "ErrorResponse", });
    internal_static_jraft_SnapshotRequest_descriptor = getDescriptor().getMessageTypes().get(6);
    internal_static_jraft_SnapshotRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_SnapshotRequest_descriptor, new String[] { "GroupId", "PeerId", });
    internal_static_jraft_ResetPeerRequest_descriptor = getDescriptor().getMessageTypes().get(7);
    internal_static_jraft_ResetPeerRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_ResetPeerRequest_descriptor, new String[] { "GroupId", "PeerId",
            "OldPeers", "NewPeers", });
    internal_static_jraft_TransferLeaderRequest_descriptor = getDescriptor().getMessageTypes().get(8);
    internal_static_jraft_TransferLeaderRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_TransferLeaderRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "PeerId", });
    internal_static_jraft_GetLeaderRequest_descriptor = getDescriptor().getMessageTypes().get(9);
    internal_static_jraft_GetLeaderRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_GetLeaderRequest_descriptor, new String[] { "GroupId", "PeerId", });
    internal_static_jraft_GetLeaderResponse_descriptor = getDescriptor().getMessageTypes().get(10);
    internal_static_jraft_GetLeaderResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_GetLeaderResponse_descriptor, new String[] { "LeaderId", "ErrorResponse", });
    internal_static_jraft_GetPeersRequest_descriptor = getDescriptor().getMessageTypes().get(11);
    internal_static_jraft_GetPeersRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_GetPeersRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "OnlyAlive", });
    internal_static_jraft_GetPeersResponse_descriptor = getDescriptor().getMessageTypes().get(12);
    internal_static_jraft_GetPeersResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_GetPeersResponse_descriptor, new String[] { "Peers", "Learners",
            "ErrorResponse", });
    internal_static_jraft_AddLearnersRequest_descriptor = getDescriptor().getMessageTypes().get(13);
    internal_static_jraft_AddLearnersRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_AddLearnersRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "Learners", });
    internal_static_jraft_RemoveLearnersRequest_descriptor = getDescriptor().getMessageTypes().get(14);
    internal_static_jraft_RemoveLearnersRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_RemoveLearnersRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "Learners", });
    internal_static_jraft_ResetLearnersRequest_descriptor = getDescriptor().getMessageTypes().get(15);
    internal_static_jraft_ResetLearnersRequest_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_ResetLearnersRequest_descriptor, new String[] { "GroupId", "LeaderId",
            "Learners", });
    internal_static_jraft_LearnersOpResponse_descriptor = getDescriptor().getMessageTypes().get(16);
    internal_static_jraft_LearnersOpResponse_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_jraft_LearnersOpResponse_descriptor, new String[] { "OldLearners", "NewLearners",
            "ErrorResponse", });
    RpcRequests.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
