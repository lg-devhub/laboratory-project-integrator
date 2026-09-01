# 🧪 Sistema de Gestão de Laboratórios e Ativos

> Sistema web desenvolvido para facilitar o gerenciamento de laboratórios, equipamentos, ativos e ordens de serviço.

---

## 📋 Sobre o Projeto

Este projeto é desenvolvido como parte do **Projeto Integrador da Faculdade de Gestão de Laboratórios e Ativos**.

A organização atualmente realiza o controle de equipamentos e ordens de serviço por meio de registros físicos e processos manuais, o que pode ocasionar perda de informações, inconsistências, retrabalho e dificuldade na rastreabilidade dos ativos.

A proposta é desenvolver uma plataforma web capaz de **centralizar, organizar e facilitar o gerenciamento dos ativos e das ordens de serviço**.

---

## 🎯 Objetivos

### Objetivo Geral

Desenvolver um sistema web para gerenciamento de laboratórios, equipamentos, ativos e ordens de serviço, proporcionando maior organização, rastreabilidade e eficiência.

### Objetivos Específicos

- 📦 Cadastrar e gerenciar equipamentos;
- 🗂️ Organizar equipamentos por categorias e tipos;
- 📍 Controlar a localização dos ativos;
- 🔧 Registrar e acompanhar ordens de serviço;
- 🛠️ Controlar manutenções;
- 📋 Manter histórico dos equipamentos;
- 👥 Gerenciar usuários e responsáveis;
- 🔎 Facilitar a consulta das informações;
- 📊 Centralizar os dados;
- 💰 Reduzir custos e retrabalho.

---

## 💡 Benefícios Esperados

- Redução de inconsistências e falhas nos registros;
- Maior agilidade no acesso às informações;
- Melhor organização e localização dos ativos;
- Diminuição do retrabalho;
- Centralização dos dados;
- Melhoria na tomada de decisões;
- Redução de custos operacionais.

---

# 🏗️ Arquitetura do Projeto

O sistema será desenvolvido utilizando:

- **Front-end:** Vue.js
- **Back-end:** Java + Spring Boot
- **Banco de Dados:** MySQL

### Estrutura geral

<p align="center">
  <img src="/project-miniMap.png" alt="Estrutura do projeto Front-end e Back-end" width="1000">
</p>

---

# 🗄️ Estrutura do Banco de Dados

A estrutura inicial do banco de dados foi planejada para organizar os principais elementos envolvidos na gestão dos laboratórios, ativos e ordens de serviço.

<p align="center">
  <img src="/miniMap-DB.png" alt="Estrutura do banco de dados" width="1000">
</p>

> A estrutura poderá ser alterada durante o desenvolvimento conforme os requisitos e regras de negócio forem definidos.

---

# 📦 Principais Módulos

```text
Sistema
│
├── 🔐 Autenticação
│
├── 📊 Dashboard
│
├── 🧰 Ativos
│   ├── Cadastro
│   ├── Consulta
│   ├── Edição
│   ├── Exclusão
│   ├── Localização
│   └── Histórico
│
├── 🏢 Laboratórios
│   ├── Cadastro
│   ├── Consulta
│   └── Equipamentos
│
├── 🗂️ Categorias
│   ├── Categorias
│   └── Tipos de equipamento
│
├── 🔧 Ordens de Serviço
│   ├── Abertura
│   ├── Consulta
│   ├── Atualização
│   ├── Status
│   └── Histórico
│
├── 👥 Usuários
│   ├── Cadastro
│   ├── Perfis
│   └── Permissões
│
└── 📈 Relatórios
    ├── Ativos
    ├── Manutenções
    └── Ordens de Serviço