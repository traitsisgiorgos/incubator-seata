/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.namingserver.pojo;


import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.metadata.namingserver.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ClusterData extends AbstractClusterData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterData.class);
    private String clusterName;
    private String clusterType;
    private final Map<String, Unit> unitData;


    public ClusterData() {
        unitData = new ConcurrentHashMap<>(32);
    }

    public ClusterData(String clusterName) {
        unitData = new ConcurrentHashMap<>(32);
        this.clusterName = clusterName;
    }

    public ClusterData(String clusterName, String clusterType) {
        unitData = new ConcurrentHashMap<>(32);
        this.clusterName = clusterName;
        this.clusterType = clusterType;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }


    public Map<String, Unit> getUnitData() {
        return unitData;
    }

    public void removeInstance(Node node, String unitName) {
        Unit unit = unitData.get(unitName);
        if (Objects.isNull(unit)) {
            LOGGER.warn("unit {} is null", unitName);
            return;
        }
        unit.removeInstance(node);
        if (unit.getNamingInstanceList() == null || unit.getNamingInstanceList().size() == 0) {
            unitData.remove(unitName);
        }
    }

    public List<Node> getInstanceList() {
        return unitData.values().stream()
                .map(Unit::getNamingInstanceList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public Cluster getClusterByUnit(String unitName) {
        Cluster clusterResponse = new Cluster();
        clusterResponse.setClusterName(clusterName);
        clusterResponse.setClusterType(clusterType);
        if (unitName == null || unitName.equals("")) {
            clusterResponse.setUnitData(new ArrayList<>(unitData.values()));
        } else {
            List<Unit> unitList = new ArrayList<>();
            Optional.ofNullable(unitData.get(unitName)).ifPresent(unitList::add);
            clusterResponse.setUnitData(unitList);
        }

        return clusterResponse;
    }

    @Override
    public boolean registerInstance(NamingServerNode instance, String unitName) {
        // refresh node weight
        Object weightValue = instance.getMetadata().get("weight");
        if (weightValue != null) {
            instance.setWeight(Double.parseDouble(String.valueOf(weightValue)));
            instance.getMetadata().remove("weight");
        }

        Unit unit = unitData.get(unitName);
        if (unit == null) {
            unit = new Unit();
            List<Node> instances = new ArrayList<>();
            instances.add(instance);
            unit.setUnitName(unitName);
            unit.setNamingInstanceList(instances);
            unitData.put(unitName, unit);
            return true;
        }
        return unit.addInstance(instance);
    }


}
