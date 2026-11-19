# XKLEscavadora 1.0.0

Plugin Bukkit/Paper para a Picareta Escavadora 3x3 do XKL Gamer.

## Receita configurada

| Diamante | Barra de Netherita | Diamante |
|---|---|---|
| Fragmento de Ametista | Vara de Brisa | Fragmento de Ametista |
| vazio | Vara de Brisa | vazio |

## Funcionamento

- A ferramenta é uma picareta de netherita identificada por PersistentDataContainer (não basta renomear uma picareta comum).
- Minera os blocos de uma área 3x3 perpendicular ao olhar do jogador.
- Os blocos adicionais são processados por eventos `BlockBreakEvent`, permitindo que plugins de proteção cancelem a quebra.
- Limita a mineração aos blocos da tag `MINEABLE_PICKAXE` e ignora blocos com estado especial (por exemplo, baús e fornalhas).
- A ferramenta recebe Eficiência V e Inquebrável III.

## Compilar

Requer JDK 25 e Gradle 9 ou superior. O projeto usa Paper API `26.3.build.+`.

Na pasta do projeto:

```bash
gradle build
```

O JAR será gerado em `build/libs/XKLEscavadora-1.0.0.jar`.

## Instalar

1. Faça backup do servidor.
2. Envie o JAR para `plugins/`.
3. Reinicie o servidor (não use apenas `/reload`).
4. Confira `/plugins` e teste a receita em uma área sem construções.

## Observação de compatibilidade

A integração com proteção é feita pelo evento Bukkit padrão. A confirmação final com EasyClaim deve ser feita no servidor de teste: se o EasyClaim cancelar `BlockBreakEvent` para os blocos adicionais, a escavadora respeitará esse cancelamento. O plugin ainda não foi compilado nem testado dentro do servidor real.
