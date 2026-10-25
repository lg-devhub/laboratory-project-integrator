import type { Usuario } from './usuario'
export interface UsuarioGestao extends Usuario { ativo: boolean; versao: number }
export interface UsuarioDados { nome: string; login: string; perfil: Usuario['perfil']; senha: string | null; versao?: number }
