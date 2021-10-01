/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.TSFFormat;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.util.lang.Util;

import java.io.*;
import java.util.*;

/**
 *
 */
public class IndexInfo {

    public static class ChildInfo implements Comparable<ChildInfo> {
        public boolean isFile;
        public int eid;
        public long ts;
        public long version;

        public ChildInfo(boolean isFile, int eid, long ts, long version) {
            this.isFile = isFile;
            this.eid = eid;
            this.ts = ts;
            this.version = version;
        }

        public int getEid() {
            return eid;
        }

        @Override
        public boolean      equals(Object obj) {
            if (!(obj instanceof ChildInfo))
                return false;

            ChildInfo info = (ChildInfo) obj;
            return (isFile == info.isFile && eid == info.eid && ts == info.ts);
        }

        public String       getName() {
            return isFile ? TSNames.buildFileName(eid) : TSNames.buildFolderName(eid);
        }

        @Override
        public int compareTo(ChildInfo o) {
            return Util.compare(ts, o.ts);
        }
    }

    public static class EntityInfo implements Comparable<EntityInfo> {
        public final int entity;
        public int firstId;
        public int lastId;

        public EntityInfo(int entity, int firstId, int lastId) {
            this.entity = entity;
            this.firstId = firstId;
            this.lastId = lastId;
        }

        @Override
        public int compareTo(EntityInfo o) {
            return Util.compare(entity, o.entity);
        }
    }

    private int formatVersion;
    private long version;
    private int nextChildId;

    private ArrayList<ChildInfo> childrenInfo = new ArrayList<>();
    private ArrayList<EntityInfo> entityInfo = new ArrayList<>();

    public List<ChildInfo>      getChildrenInfo() {
        return childrenInfo;
    }

    public void                 setChildrenInfo(ArrayList<ChildInfo> childrenInfo) {
        this.childrenInfo = childrenInfo;
    }

    public boolean              hasChildren() {
        return childrenInfo != null && childrenInfo.size() > 0;
    }

    public ChildInfo        getChild(int eid) {
        for (ChildInfo child : childrenInfo) {
            if (child.eid == eid)
                return child;
        }

        return null;
    }

    public boolean        hasChild(int eid) {
        for (ChildInfo child : childrenInfo) {
            if (child.eid == eid)
                return true;
        }

        return false;
    }

    public List<EntityInfo> getEntitiesInfo() {
        return entityInfo;
    }

//    public void setChildrenInfo(ArrayList<ChildInfo> childrenInfo) {
//        this.childrenInfo = childrenInfo;
//    }

    public void setEntityInfo(ArrayList<EntityInfo> entityInfo) {
        this.entityInfo = entityInfo;
    }

    public void saveTo(AbstractPath path) throws IOException {

        AbstractPath tmpPath = path.getParentPath().append(TSNames.TMP_PREFIX + path.getName());
        tmpPath.deleteIfExists();

        // remove .save file to be able to finalize index
        AbstractPath savePath = path.getParentPath().append(TSNames.SAVE_PREFIX + path.getName());
        savePath.deleteIfExists();

        // calculate next child id as max value
        Optional<ChildInfo> maxed = childrenInfo.stream().max(Comparator.comparing(ChildInfo::getEid));

        int nextId = maxed.map(childInfo -> childInfo.eid + 1).orElse(0);
        this.nextChildId = Math.max(nextId, nextChildId);

        //System.out.println("Set last child id: " + nextChildId);

        //PDSImpl.LOGGER.warn("Saving index file to: " + tmpPath);

        try (OutputStream os = new BufferedOutputStream (tmpPath.openOutput (0))) {
            DataOutputStream        dos = new DataOutputStream (os);

            dos.writeShort(TSFFormat.INDEX_FORMAT_VERSION);

            //PDSImpl.LOGGER.warn("Saving version: " + version);

            dos.writeLong(version);
            dos.writeShort(this.nextChildId);
            dos.writeShort(childrenInfo.size());

            for (ChildInfo info : childrenInfo) {
                dos.writeBoolean(info.isFile);
                dos.writeShort(info.eid);
                dos.writeLong(info.ts);
                dos.writeLong(info.version);
                //PDSImpl.LOGGER.warn("Write child: " + info.eid + ": " + info.ts);
            }

            Collections.sort(entityInfo); // keep it sorted
            dos.writeInt(entityInfo.size());

            for (EntityInfo info : entityInfo) {
                dos.writeInt(info.entity);
                dos.writeShort(info.firstId);
                dos.writeShort(info.lastId);
                //PDSImpl.LOGGER.warn("Write " + info.entity + ": " + info.firstId + ".." + info.lastId);
            }
        }

        TreeOps.finalize(tmpPath);
    }

    public static IndexInfo createFrom(AbstractPath path) {

        if (path.exists()) {
            IndexInfo index = new IndexInfo();

            try (DataInputStream dis = new DataInputStream(BufferedStreamUtil.wrapWithBuffered(path.openInput(0)))) {
                index.formatVersion = dis.readUnsignedShort();
                index.version = index.formatVersion >= 2 ? dis.readLong() : dis.readInt();
                index.nextChildId = dis.readUnsignedShort();
                int numChildren = dis.readUnsignedShort();

                for (int i = 0; i < numChildren; ++i) {
                    index.childrenInfo.add(new ChildInfo(
                            dis.readBoolean(),
                            dis.readUnsignedShort(),
                            dis.readLong(),
                            index.formatVersion >= 2 ? dis.readLong() : 0));
                }

                int numEntities = dis.readInt();
                for (int i = 0; i < numEntities; ++i) {
                    index.entityInfo.add(new EntityInfo(
                            dis.readInt(),
                            dis.readUnsignedShort(),
                            dis.readUnsignedShort()
                    ));
                }
            } catch (IOException e) {
                PDSImpl.LOGGER.warn("Cannot read file %s: %s").with(path).with(e);
                return null;
            }

            return index;
        }

        return null;
    }

    private void fixEntity(ChildInfo curChild, List<EntityInfo> entitiesToRemove) throws IOException {

        for (IndexInfo.EntityInfo curEntity : entityInfo) {

            for (int j = 0; j < childrenInfo.size(); ++j) {
                ChildInfo child = childrenInfo.get(j);

                if (child.eid == curEntity.firstId && child.eid == curChild.eid) {
                    if (j + 1 < childrenInfo.size()) {
                        curEntity.firstId = childrenInfo.get(j + 1).eid;
                        break;
                    }
                }

                if (child.eid == curEntity.lastId && child.eid == curChild.eid) {
                    if (j - 1 >= 0) {
                        curEntity.lastId = childrenInfo.get(j - 1).eid;
                        break;
                    }
                }

                if (childrenInfo.size() == 1 &&
                        child.eid == curEntity.firstId &&
                        child.eid == curEntity.lastId) {
                    entitiesToRemove.add(curEntity);
                    break;
                }
            }
        }
    }

    public void removeAll(List<ChildInfo> childrenToRemove) throws IOException {
        List<EntityInfo> entitiesToRemove = new ArrayList<>();

        for (ChildInfo curChild : childrenToRemove)
            fixEntity(curChild, entitiesToRemove);

        childrenInfo.removeAll(childrenToRemove);
        entityInfo.removeAll(entitiesToRemove);
    }

    public EntityInfo     findEntity(int entity) {
        for (EntityInfo e : entityInfo) {
            if (e.entity == entity)
                return e;
        }

        return null;
    }

    public boolean             addChild(ChildInfo info) {
        if (hasChild(info.eid))
            return false;

        // keep it sorted
        int index = 0;
        while (index < childrenInfo.size() && info.ts > childrenInfo.get(index).ts)
            index++;

        if (index >= childrenInfo.size())
            childrenInfo.add(info);
        else
            childrenInfo.add(index, info);

        return true;
    }

    public void             addEntity(int entity, int fileId) {
        entityInfo.add(new EntityInfo(entity, fileId, fileId));
    }

    public long             getVersion() {
        return version;
    }

    public void             setVersion(long version) {
        this.version = version;
    }

    public int              getFormatVersion() {
        return formatVersion;
    }
}