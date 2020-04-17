FROM openjdk:11

# LibreOffice installation
RUN apt-get update && \
apt-get install -y --no-install-recommends libxinerama1 libdbus-1-3 \
libdbus-glib-1-2 libcairo2 libcups2 libsm6 && \
curl -L -O https://download.documentfoundation.org/libreoffice/stable/6.4.2/deb/x86_64/LibreOffice_6.4.2_Linux_x86-64_deb.tar.gz && \
tar -xzvf LibreOffice_6.4.2_Linux_x86-64_deb.tar.gz && \
dpkg -i LibreOffice_6.4.2.2_Linux_x86-64_deb/DEBS/*.deb && \
ln -s /usr/local/bin/libreoffice6.4 /usr/local/bin/libreoffice

# The application
COPY libreoffice/registrymodifications.xcu /root/.config/libreoffice/4/user/
COPY target/etp-backend.jar /target/
ENTRYPOINT ["java", "-cp", "target/etp-backend.jar", "clojure.main", "-m", "solita.etp.core"]
