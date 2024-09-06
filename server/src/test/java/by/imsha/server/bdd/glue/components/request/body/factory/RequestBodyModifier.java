package by.imsha.server.bdd.glue.components.request.body.factory;


import com.fasterxml.jackson.databind.node.ObjectNode;

public interface RequestBodyModifier {

    ObjectNode apply(ObjectNode jsonNode) throws RuntimeException;
}
