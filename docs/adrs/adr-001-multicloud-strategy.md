# ADR-001: Estrategia Multi-Nube con Abstracción Completa

## Estado
Propuesto

## Contexto
La plataforma de Open Finance debe desplegarse en AWS, Azure y GCP. Se necesita una estrategia que permita que el mismo código de infraestructura funcione en las tres nubes sin duplicación significativa.

## Decisión
Usar **Terraform con módulos abstractos** que encapsulan las diferencias entre proveedores. Cada módulo expone una interfaz común y la implementación interna varía según el provider seleccionado.

```
modules/
├── kubernetes/    → EKS | AKS | GKE
├── networking/    → VPC AWS | VNet Azure | VPC GCP
├── database/      → RDS | Azure DB | Cloud SQL
└── security/      → KMS AWS | Key Vault | Cloud KMS
```

Se usa una variable `cloud_provider` que determina qué implementación se activa.

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| Módulos separados por nube | Simple, claro | Duplicación, difícil mantener |
| Pulumi | Multi-lenguaje, tipado | Menor ecosistema, curva de aprendizaje |
| Crossplane | K8s-native | Complejidad operativa, menos maduro |
| **Terraform abstracto** | Ecosistema maduro, portable, declarativo | Requiere diseño cuidadoso de interfaces |

## Consecuencias
- Se requiere diseñar interfaces de módulos antes de implementar
- Los módulos deben testarse en las tres nubes
- Se gana portabilidad real y consistencia entre ambientes
- El equipo debe conocer Terraform y los tres proveedores

## Riesgos
- Algunas features son específicas de una nube y no tienen equivalente directo
- El mínimo común denominador puede limitar optimizaciones específicas
- Mitigación: permitir "escape hatches" para configuraciones nube-específicas
