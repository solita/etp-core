FROM openjdk:11

# LibreOffice installation
RUN apt-get update && \
apt-get install -y --no-install-recommends libxinerama1 libdbus-1-3 libx11-xcb1 \
libdbus-glib-1-2 libcairo2 libcups2 libsm6 && \
curl -L -O https://download.documentfoundation.org/libreoffice/stable/7.1.0/deb/x86_64/LibreOffice_7.1.0_Linux_x86-64_deb.tar.gz && \
tar -xzvf LibreOffice_7.1.0_Linux_x86-64_deb.tar.gz && \
dpkg -i LibreOffice_7.1.0.3_Linux_x86-64_deb/DEBS/*.deb && \
ln -s /usr/local/bin/libreoffice7.1 /usr/local/bin/libreoffice

# The application
COPY libreoffice/ /etp/libreoffice/
COPY target/etp-backend.jar /etp/target/
COPY start.sh /etp/

WORKDIR /etp
ENTRYPOINT ["bash", "./start.sh"]
