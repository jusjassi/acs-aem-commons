/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.util.visitors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.sling.api.resource.Resource;

public class SimpleFilteringResourceVisitor {

    public enum TraversalMode {
        DEPTH, BREADTH
    }

    TraversalMode mode = TraversalMode.BREADTH;
    BiConsumer<Map.Entry<String, Object>, Integer> propertyVisitor = null;
    BiConsumer<Resource, Integer> resourceVisitor = null;
    BiConsumer<Resource, Integer> leafVisitor = null;
    LinkedList<Resource> stack = new LinkedList<>();
    Function<String, Boolean> propertyFilter = s -> true;
    Function<Resource, Boolean> traversalFilter = r -> true;

    public void setPropertyFilter(Function<String, Boolean> filter) {
        propertyFilter = filter;
    }

    public final void setTraversalFilter(Function<Resource, Boolean> filter) {
        traversalFilter = filter;
    }

    public final void setResourceVisitor(BiConsumer<Resource, Integer> handler) {
        resourceVisitor = handler;
    }

    public final void setLeafVisitor(BiConsumer<Resource, Integer> handler) {
        leafVisitor = handler;
    }

    public final void setPropertyVisitor(BiConsumer<Map.Entry<String, Object>, Integer> handler) {
        propertyVisitor = handler;
    }

    public final void setBreadthFirstMode() {
        mode = TraversalMode.BREADTH;
    }

    public final void setDepthFirstMode() {
        mode = TraversalMode.DEPTH;
    }

    public void accept(final Resource head) {
        if (head == null) {
            return;
        }
        
        stack.clear();
        stack.add(head);

        int headLevel = getDepth(head.getPath());

        while (!stack.isEmpty()) {
            Resource res = stack.poll();

            int level = getDepth(res.getPath()) - headLevel;

            if (propertyVisitor != null) {
                res.getValueMap().entrySet().stream()
                        .filter(e -> propertyFilter.apply(e.getKey()))
                        .forEach(entry -> propertyVisitor.accept(entry, level));
            }

            if (traversalFilter == null || traversalFilter.apply(res)) {
                if (resourceVisitor != null) {
                    resourceVisitor.accept(res, level);
                }
                switch (mode) {
                    case BREADTH:
                        stack.addAll(toList(res.getChildren()));
                        break;
                    default:
                        stack.addAll(0, toList(res.getChildren()));
                }
            } else if (leafVisitor != null) {
                leafVisitor.accept(res, level);
            }
        }
    }

    public static <T> List<T> toList(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
    }
    
    public static int getDepth(String path) {
        return (int) path.chars().filter(c -> c == '/').count() - 1;
    }    
}