# TP1

## Enunciado

### Requerimientos Funcionales

1. Se debe proveer acceso via internet a ciertos programas radiales.
2. Los programas de radio son en vivo y los usuarios pueden decidir escuchar uno o varios a la vez mediante distintos dispositivos.
3. Existe un límite de 3 canales de radio conectados por usuario, siendo que se considera proveer un servicio ilimitado en el futuro para usuarios premium.
4. Las estaciones de radio pueden enviar el contenido de la emisión al sistema, y así disponibilizarlo al público.
5. Los administradores del sistema pueden consultar estadísticas de uso, incluyendo:
- Cantidad de usuarios conectados por radio.
- Usuarios con mayor cantidad de horas de reproducción.

### Requerimientos No Funcionales

1. Se estima una cantidad de usuarios concurrentes muy elevada en todo momento.
2. Debido al reducido mercado de radios, se espera contar con una cantidad reducida de emisoras.
3. Se debe almacenar una entrada de log indicando conexión y desconexión de cada usuario a una determinada radio.
4. La programación debe permitir la distribución de sus componentes y la comunicación de información mediante colas persistentes.
5. El monitoreo de los flujos de información es clave para garantizar la performance del sistema.

## Problemas y decisiones decisiones

Tal como se especifica en el requisito no funcinal #4, se opta por usar un message broker. Existen dos alternativas, es decir, dos implementaciones populares de broker de messages (colas persistentes). Por un lado tenemos **Apache Kafka** y por el otro lado, **RabbitMQ**. Ambas herramientas pueden solucionar varios de los requisitos. Pero por el requisito no funcional #5 debemos seleccionar RabbitMQ ya que posee un monitor propio con varias herramientas.
