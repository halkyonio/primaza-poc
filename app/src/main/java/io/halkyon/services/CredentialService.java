package io.halkyon.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import io.halkyon.model.Credential;
import io.halkyon.model.CredentialParameter;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.CredentialRequest;
import io.sundr.utils.Strings;

@ApplicationScoped
public class CredentialService {

    private static final Logger LOG = Logger.getLogger(CredentialService.class);

    @Transactional
    public void doSave(Credential credential) {
        if (credential.id != null) {
            Credential edited = credential;
            credential = Credential.findById(credential.id);
            if (credential == null) {
                throw new NotFoundException(String.format("Credential not found for id: %d%n", credential.id));
            }
            credential = mergeEntities(credential, edited);

        }
        credential.persist();
    }

    private Credential mergeEntities(Credential old, Credential edited) {
        old.name = !old.name.equals(edited.name) ? edited.name : old.name;
        old.username = !old.username.equals(edited.username) ? edited.username : old.username;
        old.password = !old.password.equals(edited.password) ? edited.password : old.password;
        old.vaultKvPath = old.vaultKvPath != null && !old.vaultKvPath.equals(edited.vaultKvPath) ? edited.vaultKvPath
                : old.vaultKvPath;
        old.service = old.service != null && !old.service.equals(edited.service) ? edited.service : old.service;

        old.params.clear();
        if (edited.params != null) {
            for (CredentialParameter cp : edited.params) {
                CredentialParameter paramEntity = new CredentialParameter();
                paramEntity.credential = cp.credential;
                paramEntity.paramName = cp.paramName;
                paramEntity.paramValue = cp.paramValue;
                old.params.add(paramEntity);
            }
        }

        return old;
    }

    public Credential initializeCredential(CredentialRequest request) {
        Credential credential = new Credential();
        credential.name = request.name;
        credential.type = request.type;
        credential.username = request.username;
        credential.password = request.password;
        if (request.vaultKvPath != null) {
            Pattern stringPattern = Pattern.compile("^/*");
            Matcher matcher = stringPattern.matcher(request.vaultKvPath);
            if (Strings.isNotNullOrEmpty(request.vaultKvPath) && matcher.find()) {
                request.vaultKvPath = matcher.replaceFirst("");
            }
            credential.vaultKvPath = request.vaultKvPath;
        }
        credential.service = Service.findById(request.serviceId);
        credential.params.clear();
        if (request.params != null) {
            for (String param : request.params) {
                String[] nameValue = param.split(":");
                if (nameValue.length == 2) {
                    CredentialParameter paramEntity = new CredentialParameter();
                    paramEntity.credential = credential;
                    paramEntity.paramName = nameValue[0];
                    paramEntity.paramValue = nameValue[1];
                    credential.params.add(paramEntity);
                }
            }
        }
        return credential;
    }

}
