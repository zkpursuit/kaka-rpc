package com.kaka.rpc.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

/**
 * key、value双向映射 <br>
 * <p>key映射多个value</p>
 * <p>value映射多个key</p>
 *
 * @param <K> key类型限定
 * @param <V> value类型限定
 * @author zkpursuit
 */
class BiMap<K, V> {

    private final Map<K, Set<V>> keyValueSetMap;
    private final Map<V, Set<K>> valueKeySetMap;

    public BiMap() {
        keyValueSetMap = new ConcurrentHashMap<>();
        valueKeySetMap = new ConcurrentHashMap<>();
    }

    /**
     * 增加元素
     */
    public void put(K key, V value) {
        Set<V> vSet = keyValueSetMap.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>());
        vSet.add(value);
        Set<K> kSet = valueKeySetMap.computeIfAbsent(value, v -> new CopyOnWriteArraySet<>());
        kSet.add(key);
    }

    /**
     * 移除key、value
     */
    public void remove(K key, V value) {
        if (keyValueSetMap.containsKey(key)) {
            Set<V> vSet = keyValueSetMap.get(key);
            vSet.remove(value);
            if (vSet.isEmpty()) {
                keyValueSetMap.remove(key);
            }
        }
        if (valueKeySetMap.containsKey(value)) {
            Set<K> kSet = valueKeySetMap.get(value);
            kSet.remove(key);
            if (kSet.isEmpty()) {
                valueKeySetMap.remove(value);
            }
        }
    }

    /**
     * 移除key
     */
    public void removeKey(K key) {
        if (keyValueSetMap.containsKey(key)) {
            Set<V> vSet = keyValueSetMap.get(key);
            vSet.forEach((V v) -> {
                Set<K> kSet = valueKeySetMap.get(v);
                if (kSet != null) {
                    kSet.remove(key);
                    if (kSet.isEmpty()) {
                        valueKeySetMap.remove(v);
                    }
                }
            });
            keyValueSetMap.remove(key);
        }
    }

    public void removeValue(V value) {
        if (valueKeySetMap.containsKey(value)) {
            Set<K> kSet = valueKeySetMap.get(value);
            kSet.forEach((K k) -> {
                Set<V> vSet = keyValueSetMap.get(k);
                if (vSet != null) {
                    vSet.remove(value);
                    if (vSet.isEmpty()) {
                        keyValueSetMap.remove(k);
                    }
                }
            });
            valueKeySetMap.remove(value);
        }
    }

    /**
     * 获得Values
     */
    public Set<V> getValuesByKey(K key) {
        if (keyValueSetMap.containsKey(key)) {
            Set<V> set = keyValueSetMap.get(key);
            if (set != null) {
                Set<V> _set = new HashSet<>(set.size());
                _set.addAll(set);
                return _set;
            }
        }
        return null;
    }

    /**
     * 获得原始的Values，但绝对不要对其进行增加元素或删除元素的操作
     */
    public Set<V> getOriginalValuesByKey(K key) {
        if (keyValueSetMap.containsKey(key)) {
            return keyValueSetMap.get(key);
        }
        return null;
    }

    public Set<K> getKeysByValue(V value) {
        if (valueKeySetMap.containsKey(value)) {
            Set<K> kSet = valueKeySetMap.get(value);
            if (kSet != null) {
                Set<K> _set = new HashSet<>(kSet.size());
                _set.addAll(kSet);
                return _set;
            }
        }
        return null;
    }

    public Set<K> getOriginalKeysByValue(V value) {
        if (valueKeySetMap.containsKey(value)) {
            return valueKeySetMap.get(value);
        }
        return null;
    }

    public boolean hasKey(K key) {
        return keyValueSetMap.containsKey(key);
    }

    public boolean hasValue(V value) {
        return valueKeySetMap.containsKey(value);
    }

    public boolean has(K key, V value) {
        if (keyValueSetMap.containsKey(key)) {
            Set<V> vSet = keyValueSetMap.get(key);
            if (vSet == null || vSet.isEmpty()) {
                return false;
            }
            return vSet.contains(value);
        }
        return false;
    }

    public void clear() {
        this.keyValueSetMap.clear();
        this.valueKeySetMap.clear();
    }

    public void forEachKvMap(BiConsumer<K, Set<V>> action) {
        keyValueSetMap.forEach(action);
    }

    public void forEachVkMap(BiConsumer<V, Set<K>> action) {
        valueKeySetMap.forEach(action);
    }

}