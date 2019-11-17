/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.github.beelzebu.coins.common.dependency;

import com.github.beelzebu.coins.common.dependency.relocation.Relocation;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public enum Dependency {

    ASM("org.ow2.asm", "asm", "6.1.1"),
    ASM_COMMONS("org.ow2.asm", "asm-commons", "6.1.1"),
    JAR_RELOCATOR("me.lucko", "jar-relocator", "1.3"),
    CAFFEINE("com{}github{}ben-manes{}caffeine", "caffeine", "2.6.2", Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")),
    MARIADB_DRIVER("org{}mariadb{}jdbc", "mariadb-java-client", "2.2.3", Relocation.of("mariadb", "org{}mariadb{}jdbc")),
    MYSQL_DRIVER("mysql", "mysql-connector-java", "5.1.46", Relocation.of("mysql", "com{}mysql")),
    SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "3.21.0"),
    HIKARI("com{}zaxxer", "HikariCP", "3.2.0", Relocation.of("hikari", "com{}zaxxer{}hikari")),
    SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "1.7.25"),
    SLF4J_API("org.slf4j", "slf4j-api", "1.7.25"),
    JEDIS("redis.clients", "jedis", "3.1.0", Relocation.allOf(Relocation.of("jedis", "redis{}clients{}jedis"), Relocation.of("jedisutil", "redis{}clients{}util"), Relocation.of("commonspool2", "org{}apache{}commons{}pool2"))),
    COMMONS_POOL_2("org.apache.commons", "commons-pool2", "2.5.0", Relocation.of("commonspool2", "org{}apache{}commons{}pool2"));

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";
    private final String url;
    private final String version;
    private final List<Relocation> relocations;

    Dependency(String groupId, String artifactId, String version) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, Collections.emptyList());
    }

    Dependency(String groupId, String artifactId, String version, Relocation relocations) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, Collections.singletonList(relocations));
    }

    Dependency(String groupId, String artifactId, String version, List<Relocation> relocations) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, relocations);
    }

    Dependency(String url, String version, List<Relocation> relocations) {
        this.url = url;
        this.version = version;
        this.relocations = relocations;
    }
}
