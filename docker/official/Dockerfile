FROM rundeck/ubuntu-base

USER rundeck

COPY --chown=rundeck:root .build .
RUN java -jar rundeck.war --installonly --basedir /home/rundeck

# Directories and permissions
RUN mkdir -p container-plugins libext etc var/logs \
        # Adjust permissions for OpenShift
        && chmod -R 0775 container-plugins libext server user-assets etc var

COPY --chown=rundeck:root remco /etc/remco
COPY --chown=rundeck:root lib docker-lib
COPY --chown=rundeck:root etc etc

# Generate CDS (Class Data Sharing) archive for faster startup.
# Training run records all classes loaded during WAR install, then the archive
# is memory-mapped on subsequent starts instead of re-parsing JARs.
RUN java -XX:DumpLoadedClassList=/home/rundeck/rundeck.classlist \
        -jar rundeck.war --installonly --basedir /home/rundeck 2>/dev/null || true && \
    java -Xshare:dump -XX:SharedClassListFile=/home/rundeck/rundeck.classlist \
        -XX:SharedArchiveFile=/home/rundeck/rundeck.jsa \
        -jar rundeck.war 2>/dev/null || true && \
    rm -f /home/rundeck/rundeck.classlist

VOLUME ["/home/rundeck/server/data"]
VOLUME ["/home/rundeck/var/logs"]

EXPOSE 4440
ENTRYPOINT [ "/tini", "--", "docker-lib/entry.sh" ]
