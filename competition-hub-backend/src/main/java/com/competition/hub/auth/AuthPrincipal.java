package com.competition.hub.auth;

import com.competition.hub.store.DomainModels.Role;
//record 是 Java 16 正式发布的一种类类型，专门用来定义"纯粹装数据的不可变对象"。
public record AuthPrincipal(long id, String username, Role role) {
}

