# IoTDM: OneM2M protocols commons

This modules specifies generic interfaces and classes as templates
for implementations of protocols. So the implementation of protocol
which is implemented according to these templates should be
divided to this parts:

    Rx - The part handling received requests
     a) RxChannel
            - implements details of receiving request and sending response
            - calls RxHandler for every request received
     b) RxHandler
            - processes instances of RxRequests, calls methods of the RxRequest which
            implement the logic of the handling
     c) RxRequest
            - implements details of the handling of it's own instances
            - the handling is divided into logical steps allowing overriding of
            the implementation of specific step in child classes (these steps
            are performed in correct order by RxHandler)

     Tx - The part specifying implementation of sending of requests
      a) TxChannel
             - implements details of sending request and receiving response
             - calls TxHandler which performs the sending of request and receiving response
      b) TxHandler
             - processes instances of TxRequests, calls methods of the TxRequest which
             implements the logic of the handling
      c) TxRequest
             - implements details of the handling of it's own instances
             - the handling is divided into logical steps allowing overriding of
             the implementation of specific step in child classes (these steps
             are performed in correct order by TxHandler)
